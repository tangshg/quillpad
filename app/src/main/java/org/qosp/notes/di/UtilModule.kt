package org.qosp.notes.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
import org.qosp.notes.data.sync.nextcloud.NextcloudAPI
import org.qosp.notes.data.sync.nextcloud.NextcloudManager
import org.qosp.notes.data.sync.webdav.WebdavAPIImpl
import org.qosp.notes.data.sync.webdav.WebdavManager
import org.qosp.notes.preferences.AppPreferences
import org.qosp.notes.preferences.CloudService
import org.qosp.notes.preferences.CloudService.*
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.ui.reminders.ReminderManager
import org.qosp.notes.ui.utils.ConnectionManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

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


    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        preferenceRepository: PreferenceRepository,
        idMappingRepository: IdMappingRepository,
        nextcloudManager: NextcloudManager,
        app: Application,
    ) = SyncManager(
        preferenceRepository,
        idMappingRepository,
        ConnectionManager(context),
        nextcloudManager,
        (app as App).syncingScope
    )



//    @Singleton
//    @Provides
//    fun provideSyncProvider(
//        @ApplicationContext context: Context,
//        appPreferences: AppPreferences,
//        noteRepository: NoteRepository,
//        notebookRepository: NotebookRepository,
//        idMappingRepository: IdMappingRepository,
//        app: Application,
//    ): SyncProvider {
//
//        val cloudService = appPreferences.cloudService
//
//        return when (cloudService) {
//            NEXTCLOUD -> NextcloudManager(
//                nextcloudAPI(context), // 假设你需要传递一个NextcloudAPI实例
//                noteRepository,
//                notebookRepository,
//                idMappingRepository
//            )
//            WEBDAV -> WebdavManager(
//                WebdavAPIImpl(), // 假设WebdavAPIImpl没有额外的依赖，可以直接实例化
//                noteRepository,
//                notebookRepository,
//                idMappingRepository
//            )
//            DISABLED -> throw IllegalArgumentException("Sync service is disabled.")
//        }
//    }

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
