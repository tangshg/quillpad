package org.qosp.notes.di

import android.app.Application
import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.qosp.notes.App
import org.qosp.notes.BuildConfig
import org.qosp.notes.components.MediaStorageManager
import org.qosp.notes.components.backup.BackupManager
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.repo.ReminderRepository
import org.qosp.notes.data.repo.TagRepository
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.data.sync.core.SyncProvider
import org.qosp.notes.data.sync.nextcloud.NextcloudManager
import org.qosp.notes.data.sync.webdav.WebdavManager
import org.qosp.notes.preferences.CloudService
import org.qosp.notes.preferences.CloudService.*
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.ui.reminders.ReminderManager
import org.qosp.notes.ui.utils.ConnectionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    private  val tangshgTAG = "tangshgUtilModule"

    @Provides
    @Singleton
    fun provideMediaStorageManager(
        @ApplicationContext context: Context,
        noteRepository: NoteRepository,
    ) = MediaStorageManager(context, noteRepository, App.MEDIA_FOLDER)

    @Provides
    @Singleton
    fun provideReminderManager(
        @ApplicationContext context: Context,
        reminderRepository: ReminderRepository,
    ) = ReminderManager(context, reminderRepository)


    //这里函数的最主要是为了向 provider 注入 SyncManager
    //提供 NextcloudManager 或者 WebdavManager

    //不可以通过挂起函数来获取，因为函数是挂起函数，不能在主线程中调用

    //为什么 NextcloudManager不报错，webdavManager 报错
    @Singleton
    @Provides
     fun provideSyncProvider(
        nextcloudManager: NextcloudManager,
        webdavManager: WebdavManager,

       preferenceRepository: PreferenceRepository,
         ): SyncProvider
    {
        return runBlocking{
            val cloudService = preferenceRepository.getCloudService().first()

            Log.i(tangshgTAG,"获取的云服务提供商 $cloudService")

            val syncProvider: SyncProvider = when (cloudService) {
                NEXTCLOUD -> nextcloudManager
                WEBDAV -> webdavManager
                DISABLED -> throw IllegalStateException("No cloud service selected")
            }

            Log.i(tangshgTAG,"提供的云服务商是 $syncProvider")
            syncProvider
        }

    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        preferenceRepository: PreferenceRepository,
        idMappingRepository: IdMappingRepository,
        syncProvider: SyncProvider,
        app: Application,
    ) = SyncManager(
        preferenceRepository,
        idMappingRepository,
        ConnectionManager(context),
        syncProvider,
        (app as App).syncingScope
    )

//    fun provideCloudManager(
//        @ApplicationContext context: Context,
//        preferenceRepository: PreferenceRepository,
//        app: Application,
//    ): CloudManager {
//        val cloudService = preferenceRepository.getCloudService()
//        return when (cloudService) {
//            NEXTCLOUD -> NextcloudManager(context, preferenceRepository, (app as App).syncingScope)
//            WEBDAV -> WebdavManager(context, preferenceRepository, (app as App).syncingScope)
//            DISABLED -> throw IllegalArgumentException("Sync service is disabled.")
//        }
//    }


    //TODO 这里是最重要的，怎么做，才能绑定 SyncManager

//    @Provides
//    @Singleton
//    fun provideWebdavSyncManager(
//        @ApplicationContext context: Context,
//        preferenceRepository: PreferenceRepository,
//        idMappingRepository: IdMappingRepository,
//        webdavManager: WebdavManager,
//        app: Application,
//    ) = SyncManager(
//        preferenceRepository,
//        idMappingRepository,
//        ConnectionManager(context),
//        webdavManager,
//        (app as App).syncingScope
//    )


    @Provides
    @Singleton
    fun provideBackupManager(
        noteRepository: NoteRepository,
        notebookRepository: NotebookRepository,
        tagRepository: TagRepository,
        reminderRepository: ReminderRepository,
        idMappingRepository: IdMappingRepository,
        reminderManager: ReminderManager,
        @ApplicationContext context: Context,
    ) = BackupManager(
        BuildConfig.VERSION_CODE,
        noteRepository,
        notebookRepository,
        tagRepository,
        reminderRepository,
        idMappingRepository,
        reminderManager,
        context
    )
}
