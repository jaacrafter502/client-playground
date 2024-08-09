package io.emja.client

import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@ShellComponent
class MyShellCommands {

    @ShellMethod("Say hello to someone")
    fun sayHello(name: String): String {
        return "Hello, $name!"
    }

}