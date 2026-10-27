![trchat2](https://user-images.githubusercontent.com/34670283/160282372-a048a12c-911a-40da-8dce-a737c9596055.png)

[![Version](https://img.shields.io/github/v/release/TrPlugins/TrChat?logo=VirusTotal&style=for-the-badge)](https://github.com/FlickerProjects/TrChat/releases)
[![Issues](https://img.shields.io/github/issues/TrPlugins/TrChat?logo=StackOverflow&style=for-the-badge)](https://github.com/FlickerProjects/TrChat/issues)
[![Last Commit](https://img.shields.io/github/last-commit/TrPlugins/TrChat?logo=ApacheRocketMQ&style=for-the-badge&color=1e90ff)](https://github.com/FlickerProjects/TrChat/commits/v2)
[![Downloads](https://img.shields.io/github/downloads/TrPlugins/TrChat/total?style=for-the-badge&logo=docusign)](https://github.com/FlickerProjects/TrChat/releases)
---

### ICT 本地构建

基于上游 2.5.4，支持 Minecraft 26.x。Bukkit/Paper 与 Velocity 必须使用同一版本。

- `/room create` 创建邀请制私密房间；`/room invite <玩家>` 跨服邀请；`/room accept` 接受。
- 每个房间最多 8 人（含创建者），邀请 60 秒有效；`/room list` 查看成员，`/room leave` 退出。
- `/room` 切回房间，`/room chat <消息>` 单次发言。切换普通频道保留成员身份，切服恢复房间，代理重启后解散。
- 房间格式位于 `channels/Room.yml`，不会广播到公共频道或 Discord。默认权限 `trchat.private.room`。
- 普通玩家可分别获得 `trchat.color.anvil.*`、`trchat.color.sign.*`、`trchat.color.book.*`；聊天颜色单独授权，管理员使用 `trchat.color.*`。
- `/tell` 等已配置私聊别名指向 TrChat；显式拒绝的颜色节点和原始 `§` 输入均进行权限检查。
- 语言文件自动监听更新；Bukkit `/trchat reload` 和 Velocity `/trchatv reload` 均可刷新语言缓存。

验证：`gradlew :project:common:roomCheck :project:runtime-bukkit:chatCheck :plugin:build`。

---

### 🔔 What's new in TrChat v2?
- **Optimized performance**
- **New Channel & Format System**
- **Better compatibility with other plugins**

---

### ⛏ API usage: 
```java
public class Demo implements Listener {
    
    @EventHandler
    private void e(TrChatEvent e) {
        e.getChannel(); // 获取聊天频道
        e.setCanceled(true); // 取消发送聊天
        e.setMessage("..."); // 改变聊天内容
    }   
}
```
