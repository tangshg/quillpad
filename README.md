
### 为 quillpad 添加 webdav 服务
1. 使用 sardine-android  依赖库实现 webdav
 - [√] “同步服务”对话框中添加 “webdav”选项
 - [√] 点击“同步服务”对话框中的 “webdav”，可以切换到 webdav 的输入页面
 - [√] webdav 的输入页面，有网址输入框、账号密码输入框
 - [√] 网址输入框，可以输入网址并保存
 - [] 账号密码输入框，对输入的账号密码进行判定，如果输入正确，则保存账号密码，如果输入错误，则提示错误信息

1. 在 WebdavAccountDialog 中弹出对话框并进行简单的判空操作
2. 如果输入框不为空，将输入的账号密码传入 WebdavViewModel 中，进行身份认证
3. WebdavViewModel 中，通过 authenticate 方法进行身份认证
awdw3cwkrmmrey3y

fangshancun@gmail.com

https://dav.jianguoyun.com/dav/


//认证信息的流程

WebdavAccountDialog → WebdavViewModel → SyncManager → SyncProvider →（这是怎么来的？） WebdavManager → WebdavAPI 
