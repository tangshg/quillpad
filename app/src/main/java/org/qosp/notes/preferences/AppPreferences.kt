/**
 * AppPreferences 类用于存储应用程序的配置选项。
 * 该类为数据类（data class），提供了若干配置项的默认值。
 * 配置项包括布局模式、主题模式、深色主题模式、颜色方案、排序方法、备份策略、
 * 笔记删除时间、日期格式、时间格式、媒体文件打开方式、是否显示日期、
 * 编辑器字体大小、是否显示模式切换的浮动操作按钮、是否将无笔记本的笔记分组、
 * 已检查项的移动策略、云服务配置、同步模式、后台同步配置以及新笔记是否可同步。
 */
package org.qosp.notes.preferences

import me.msoul.datastore.defaultOf

data class AppPreferences(
    val layoutMode: LayoutMode = defaultOf(), // 布局模式，默认值
    val themeMode: ThemeMode = defaultOf(), // 主题模式，默认值
    val darkThemeMode: DarkThemeMode = defaultOf(), // 深色主题模式，默认值
    val colorScheme: ColorScheme = defaultOf(), // 颜色方案，默认值
    val sortMethod: SortMethod = defaultOf(), // 排序方法，默认值
    val backupStrategy: BackupStrategy = defaultOf(), // 备份策略，默认值
    val noteDeletionTime: NoteDeletionTime = defaultOf(), // 笔记删除时间设置，默认值
    val dateFormat: DateFormat = defaultOf(), // 日期格式，默认值
    val timeFormat: TimeFormat = defaultOf(), // 时间格式，默认值
    val openMediaIn: OpenMediaIn = defaultOf(), // 媒体文件打开方式，默认值
    val showDate: ShowDate = defaultOf(), // 是否显示日期，默认值
    val editorFontSize: FontSize = defaultOf(), // 编辑器字体大小，默认值
    val showFabChangeMode: ShowFabChangeMode = defaultOf(), // 是否显示浮动操作按钮用于切换模式，默认值
    val groupNotesWithoutNotebook: GroupNotesWithoutNotebook = defaultOf(), // 是否将没有笔记本的笔记分组，默认值
    val moveCheckedItems: MoveCheckedItems = defaultOf(), // 已检查项的移动策略，默认值
    val cloudService: CloudService = defaultOf(), // 云服务配置，默认值
    val syncMode: SyncMode = defaultOf(), // 同步模式，默认值
    val backgroundSync: BackgroundSync = defaultOf(), // 后台同步配置，默认值
    val newNotesSyncable: NewNotesSyncable = defaultOf(), // 新笔记是否可同步，默认值
)
