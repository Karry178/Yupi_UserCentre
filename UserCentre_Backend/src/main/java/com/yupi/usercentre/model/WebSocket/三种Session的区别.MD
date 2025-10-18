#     三种Session的区别：

### 1.HttpSession
```java
HttpSession session = request.getSession();
// 用于存储用户登录状态
// 可以存储在 Redis 中实现分布式
```

#### 特点：
1.用于 HTTP 请求

2.存储用户登录信息

3.可以分布式存储（Redis）
### 2.WebSocket Session（javax.websocket.Session）
```java
import javax.websocket.Session;  // ← 注解驱动用的

@OnOpen
public void onOpen(Session session) {
    // 这是 WebSocket 的 Session
}
```

#### 特点：
1.不是 HTTP Session！

2.代表一个 WebSocket 连接

3.不能存储在 Redis（是内存中的连接对象）

4.用于发送/接收 WebSocket 消息
### 3.Spring WebSocket Session（原生的WebSocketSession）
```java
import org.springframework.web.socket.WebSocketSession;  // ← Spring 原生用的

public void afterConnectionEstablished(WebSocketSession session) {
    // 这是 Spring 封装的 WebSocket Session
}
```

#### 特点：
1.Spring 对 WebSocket 连接的封装

2.功能和 javax.websocket.Session 类似

3.提供了更多 Spring 特性