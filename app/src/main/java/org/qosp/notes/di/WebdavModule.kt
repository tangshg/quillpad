package org.qosp.notes.di

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.sync.webdav.WebdavAPI
import org.qosp.notes.data.sync.webdav.WebdavConfig
import org.qosp.notes.data.sync.webdav.WebdavManager
import org.qosp.notes.data.sync.webdav.WebdavAPIImpl
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebdavModule {


    @Provides
    @Singleton
    fun provideWebdavAPI(config: WebdavConfig): WebdavAPI {
        val sardine = OkHttpSardine()
        sardine.setCredentials(config.username, config.password)
        return WebdavAPIImpl(sardine)
    }

    @Provides
    @Singleton
    fun provideWebdavManager(
        webdavAPI: WebdavAPIImpl,
        @Named(NO_SYNC) noteRepository: NoteRepository,
        @Named(NO_SYNC) notebookRepository: NotebookRepository,
        idMappingRepository: IdMappingRepository,
    ) = WebdavManager(webdavAPI, noteRepository, notebookRepository, idMappingRepository)
}
