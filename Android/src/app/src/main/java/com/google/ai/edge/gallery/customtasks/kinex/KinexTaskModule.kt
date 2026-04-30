package com.google.ai.edge.gallery.customtasks.kinex

import com.google.ai.edge.gallery.customtasks.common.CustomTask
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class KinexTaskModule {

  @Binds
  @IntoSet
  abstract fun bindKinexTask(task: KinexTask): CustomTask
}
