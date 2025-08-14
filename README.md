# CmdSender
CmdSender is a lightweight Paper/Spigot plugin that lets you schedule commands to be executed once for a specific player, either immediately if they are online or the next time they join the server.
When the target player joins, all queued commands are executed from the console with placeholders like %player% and %uuid% automatically replaced, and then removed from the queue.
If the player is already online, the command is executed instantly, and you’re notified right away.
