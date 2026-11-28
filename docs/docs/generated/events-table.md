| Event name                     | Priority order         |
| ------------------------------ | ---------------------- |
| CommandExecuteEvent            | FIRST                  |
| PlayerAvailableCommandsEvent   | FIRST                  |
| PlayerChatEvent                | FIRST                  |
| DisconnectEvent                | NORMAL                 |
| PlayerChooseInitialServerEvent | LAST (priority -32768) |
| ServerPreConnectEvent          | LAST (priority -32768) |
| GameProfileRequestEvent        | NORMAL                 |
| LoginEvent                     | LAST (priority -32768) |
| PostLoginEvent                 | FIRST                  |
| PreLoginEvent                  | LAST                   |