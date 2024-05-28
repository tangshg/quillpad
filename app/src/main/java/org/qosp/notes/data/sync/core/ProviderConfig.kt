package org.qosp.notes.data.sync.core

import org.qosp.notes.preferences.CloudService

/**
 * ProviderConfig 接口定义了云服务提供者配置的基本属性。
 * 它包含了远程地址、用户名、云服务类型以及认证所需的头信息。
 */
interface ProviderConfig {
    // 远程服务的地址
    val remoteAddress: String
    // 连接远程服务使用的用户名
    val username: String
    // 云服务类型
    val provider: CloudService
    // 认证过程中使用的头信息键值对
    val authenticationHeaders: Map<String, String>
}

