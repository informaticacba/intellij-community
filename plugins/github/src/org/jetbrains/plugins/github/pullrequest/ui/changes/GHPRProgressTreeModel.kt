// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.pullrequest.ui.changes

import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModel
import com.intellij.collaboration.ui.codereview.setupCodeReviewProgressModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.repo.GitRepository
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestFileViewedState
import org.jetbrains.plugins.github.api.data.pullrequest.isViewed
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRReviewDataProvider
import org.jetbrains.plugins.github.pullrequest.data.provider.GHPRViewedStateDataProvider

internal fun ChangesTree.showPullRequestProgress(
  parent: Disposable,
  repository: GitRepository,
  reviewData: GHPRReviewDataProvider,
  viewedStateData: GHPRViewedStateDataProvider
) {
  val model = GHPRProgressTreeModel(repository, reviewData, viewedStateData)
  Disposer.register(parent, model)

  setupCodeReviewProgressModel(parent, model)
}

private class GHPRProgressTreeModel(
  private val repository: GitRepository,
  private val reviewData: GHPRReviewDataProvider,
  private val viewedStateData: GHPRViewedStateDataProvider
) : CodeReviewProgressTreeModel(),
    Disposable {

  private var unresolvedThreadsCount: Map<String, Int> = emptyMap()
  private var filesViewedState: Map<String, GHPullRequestFileViewedState> = emptyMap()

  init {
    loadThreads()
    reviewData.addReviewThreadsListener(this) { loadThreads() }

    loadViewedState()
    viewedStateData.addViewedStateListener(this) { loadViewedState() }
  }

  private fun loadThreads() {
    reviewData.loadReviewThreads().handleOnEdt(this) { threads, _ ->
      threads ?: return@handleOnEdt // error

      unresolvedThreadsCount = threads.filter { !it.isResolved }.groupingBy { it.path }.eachCount()
      fireModelChanged()
    }
  }

  private fun loadViewedState() {
    viewedStateData.loadViewedState().handleOnEdt(this) { viewedState, _ ->
      viewedState ?: return@handleOnEdt // error

      filesViewedState = viewedState
      fireModelChanged()
    }
  }

  override fun dispose() = Unit

  override fun isRead(node: ChangesBrowserNode<*>): Boolean {
    val change = node.userObject as? Change ?: return true
    val repositoryRelativePath = VcsFileUtil.relativePath(repository.root, ChangesUtil.getFilePath(change))

    val viewedState = filesViewedState[repositoryRelativePath] ?: return true
    return viewedState.isViewed()
  }

  override fun getUnresolvedDiscussionsCount(node: ChangesBrowserNode<*>): Int {
    val change = node.userObject as? Change ?: return 0
    val repositoryRelativePath = VcsFileUtil.relativePath(repository.root, ChangesUtil.getFilePath(change))

    return unresolvedThreadsCount[repositoryRelativePath] ?: 0
  }
}