package org.qosp.notes.di

import android.app.Application
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.qosp.notes.App
import org.qosp.notes.data.repo.IdMappingRepository
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.sync.nextcloud.NextcloudAPI
import org.qosp.notes.data.sync.nextcloud.NextcloudManager
import org.qosp.notes.data.sync.webdav.WebdavAPI
import org.qosp.notes.data.sync.webdav.WebdavManager
import org.qosp.notes.ui.utils.ConnectionManager
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebdavModule {

    @Provides
    @Singleton
    fun provideWebdavManager(
        webdavAPI: WebdavAPI,
        @Named(NO_SYNC) noteRepository: NoteRepository,
        @Named(NO_SYNC) notebookRepository: NotebookRepository,
        idMappingRepository: IdMappingRepository,
    ) = WebdavManager(webdavAPI, noteRepository, notebookRepository, idMappingRepository)
}
