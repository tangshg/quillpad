package org.qosp.notes.di

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.sync.webdav.WebdavAPI
import org.qosp.notes.data.sync.webdav.WebdavConfig
import org.qosp.notes.data.sync.webdav.WebdavManager
import org.qosp.notes.data.sync.webdav.WebdavAPIImpl
import org.qosp.notes.preferences.PreferenceRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebdavModule {

    @Provides
    @Singleton
    fun provideWebdavAPI(config: WebdavConfig): WebdavAPI {
        //TODO 在这里对 sardine 进行配置是否合理？
        val sardine = OkHttpSardine()
        sardine.setCredentials(config.username, config.password)
        return WebdavAPIImpl(sardine)
    }

    @Provides
    @Singleton
    fun provideWebdavManager(
        webdavAPI: WebdavAPI,
        @Named(NO_SYNC) noteRepository: NoteRepository,
        @Named(NO_SYNC) notebookRepository: NotebookRepository,
        idMappingRepository: IdMappingRepository,
    ) = WebdavManager(webdavAPI, noteRepository, notebookRepository, idMappingRepository)

    @Singleton
    @Provides
    fun provideWebdavConfig(preferencesRepository: PreferenceRepository): WebdavConfig {
        return runBlocking {
            val url = preferencesRepository.getEncryptedString(PreferenceRepository.WEBDAV_INSTANCE_URL).first()
            val username = preferencesRepository.getEncryptedString(PreferenceRepository.WEBDAV_USERNAME).first()
            val password = preferencesRepository.getEncryptedString(PreferenceRepository.WEBDAV_PASSWORD).first()

            WebdavConfig(url, username, password)
        }
    }
}
