package org.qosp.notes.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.qosp.notes.components.StorageCleaner
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.Notebook
import org.qosp.notes.data.repo.NoteRepository
import org.qosp.notes.data.repo.NotebookRepository
import org.qosp.notes.data.repo.ReminderRepository
import org.qosp.notes.data.repo.TagRepository
import org.qosp.notes.data.sync.core.BaseResult
import org.qosp.notes.data.sync.core.SyncManager
import org.qosp.notes.preferences.*
import org.qosp.notes.ui.reminders.ReminderManager
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository,
    private val preferenceRepository: PreferenceRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderManager: ReminderManager,
    private val tagRepository: TagRepository,
    private val storageCleaner: StorageCleaner,
    private val syncManager: SyncManager,
) : ViewModel() {

    val notebooks: Flow<List<Notebook>> = notebookRepository.getAll()

    var showHiddenNotes: Boolean = false
    var notesToBackup: Set<Note>? = null
    var tempPhotoUri: Uri? = null

    fun syncAsync(): Deferred<BaseResult> {
        return syncManager.syncingScope.async {
            syncManager.sync()
        }
    }

    fun sync() {
        syncManager.syncingScope.launch {
            syncManager.sync()
        }
    }

    fun discardEmptyNotesAsync() = viewModelScope.async(Dispatchers.IO) {
        noteRepository.discardEmptyNotes()
    }

    fun deleteNotesPermanently(vararg notes: Note) = viewModelScope.launch(Dispatchers.IO) {
        notes.forEach { reminderManager.cancelAllRemindersForNote(it.id) }
        noteRepository.deleteNotes(*notes)
    }

    fun deleteNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            notes.forEach { reminderManager.cancelAllRemindersForNote(it.id) }

            when (preferenceRepository.get<NoteDeletionTime>().first()) {
                NoteDeletionTime.INSTANTLY -> {
                    noteRepository.deleteNotes(*notes)
                    storageCleaner.clean()
                }
                else -> {
                    noteRepository.moveNotesToBin(*notes)
                }
            }
        }
    }

    fun restoreNotes(vararg notes: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.restoreNotes(*notes)
        }
    }

    fun archiveNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isArchived = true,
        )
    }

    fun unarchiveNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isArchived = false,
        )
    }

    fun showNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isHidden = false,
        )
    }

    fun hideNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isHidden = true,
        )
    }

    fun pinNotes(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isPinned = !note.isPinned,
        )
    }

    fun moveNotes(notebookId: Long?, vararg notes: Note) = update(*notes) { note ->
        note.copy(
            notebookId = notebookId,
            modifiedDate = Instant.now().epochSecond,
        )
    }

    fun makeNotesSyncable(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isLocalOnly = false,
        )
    }

    fun makeNotesLocal(vararg notes: Note) = update(*notes) { note ->
        note.copy(
            isLocalOnly = true,
        )
    }

    fun duplicateNotes(vararg notes: Note) = notes.forEachAsync { note ->
        val oldId = note.id
        val cloned = note.copy(
            id = 0L,
            creationDate = Instant.now().epochSecond,
            modifiedDate = Instant.now().epochSecond,
            deletionDate = if (note.isDeleted) Instant.now().epochSecond else null
        )

        val newId = noteRepository.insertNote(cloned)
        tagRepository.copyTags(oldId, newId)
        reminderRepository.copyReminders(oldId, newId)

        reminderRepository
            .getByNoteId(newId)
            .first()
            .forEach {
                reminderManager.schedule(it.id, it.date, it.noteId)
            }
    }

    fun setLayoutMode(layoutMode: LayoutMode) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(layoutMode)
        }
    }

    fun setSortMethod(method: SortMethod) {
        viewModelScope.launch(Dispatchers.IO) {
            preferenceRepository.set(method)
        }
    }

    private inline fun update(
        vararg notes: Note,
        crossinline transform: suspend (Note) -> Note,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val notes = notes
            .map { transform(it) }
            .toTypedArray()

        noteRepository.updateNotes(*notes)
    }

    private inline fun Array<out Note>.forEachAsync(crossinline block: suspend CoroutineScope.(Note) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            forEach { block(it) }
        }
    }
}