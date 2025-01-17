// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.animation

import java.awt.AlphaComposite
import java.util.function.Consumer

class AlphaAnimationContext(private val base: AlphaComposite, val consumer: Consumer<AlphaComposite?>) {
  constructor(consumer: Consumer<AlphaComposite?>) : this(AlphaComposite.SrcOver, consumer)

  var composite: AlphaComposite? = null
    private set

  val animator = ShowHideAnimator {
    composite = when {
      it <= 0.0 -> null
      it >= 1.0 -> base
      else -> base.derive(it.toFloat())
    }
    consumer.accept(composite)
  }
}
