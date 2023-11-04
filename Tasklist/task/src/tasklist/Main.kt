package tasklist

import kotlin.system.exitProcess
import kotlinx.datetime.*
import java.lang.Exception
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private val type = Types.newParameterizedType(MutableList::class.java, TaskList.Task::class.java)
private val taskListAdapter = moshi.adapter<MutableList<TaskList.Task>>(type)
private val currentDirectory = File(System.getProperty("user.dir"))

class TaskList {

    val tasks: MutableList<TaskList.Task> = mutableListOf()

    private fun load(): MutableList<Task> {
        if (File("tasklist.json").exists()) {
            tasks.addAll(taskListAdapter.fromJson(File(currentDirectory,"tasklist.json").readText())!!)
        }
        return tasks
    }

    // Define priority levels for tasks
    enum class Priority(val color: String) {
        C(" \u001B[101m \u001B[0m "),
        H(" \u001B[103m \u001B[0m "),
        N(" \u001B[102m \u001B[0m "),
        L(" \u001B[104m \u001B[0m ")
    }

    // Define dueTags for tasks
    enum class DueTag(val color: String) {
        I( " \u001B[102m \u001B[0m "),
        T(" \u001B[103m \u001B[0m "),
        O(" \u001B[101m \u001B[0m ")
    }

    // Data class to hold task information
    data class Task(var priority: String, var dateTime: String, var tasksList: MutableList<String>)

    // Add a new task to the list
    private fun addTask() {
        val priority: String = promptForPriority()
        val date = promptForDate()
        val dateTime = promptForTime(date)
        val tasksList = promptForTasks()

        // Add the task to the task list if it has at least one task associated with it
        if (tasksList.isNotEmpty()) {
            val newTask = Task(priority, dateTime, tasksList)
            tasks.add(newTask)
        }
    }

    // Prompt the user to input the priority level of the task
    private fun promptForPriority(): String {
        while (true) {
            println("Input the task priority (C, H, N, L):")
            when (val input = readln().uppercase()) {
                "C", "H", "N", "L" -> {
                    return input
                }
            }
        }
    }

    // Prompt the user for the date and time of the task
    private fun promptForDate(): String {
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            try {
                val (year, month, day) = readln().split("-")
                val pad = { s: String -> s.padStart(2, '0') }
                LocalDate(year.toInt(), month.toInt(), day.toInt())
                return "$year-${pad(month)}-${pad(day)}"
            } catch (e: Exception) {
                println("The input date is invalid")
                continue
            }
        }
    }

    // Prompt the user for the date and time of the task
    private fun promptForTime(date: String): String {
        while (true) {
            println("Input the time (hh:mm):")
            try {
                val (hour, minute) = readln().split(":")
                val pad = { s: String -> s.padStart(2, '0') }
                val onlyDate = date.substringBefore('T')
                val (year, month, day) = onlyDate.split('-')
                LocalDateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
                return "$year-$month-${day}T${pad(hour)}:${pad(minute)}"
            } catch (e: Exception) {
                println("The input time is invalid")
                continue
            }
        }
    }

    // Prompt the user to input the tasks associated with the task number
    private fun promptForTasks(): MutableList<String> {
        val tasks = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")
        while (true) {
            val input: String = readln().trim()
            if (input.isBlank() && tasks.isEmpty()) {
                println("The task is blank")
                break
            } else if (input.isBlank()) break
            tasks.add(input)
        }
        return tasks
    }

    // Print all tasks in the list
    private fun printTasks() {
        if (tasks.isEmpty()) {
            println("No tasks have been input")
            return
        }
        val indent = "|    |            |       |   |   |"
        val horizontalLine = "+----+------------+-------+---+---+--------------------------------------------+"
        val titleLine = "| N  |    Date    | Time  | P | D |                   Task                     |"
        println("""
            $horizontalLine
            $titleLine
            $horizontalLine""".trimIndent()
        )
        for (i in tasks.indices) {
            val space = if (i >= 9) " " else "  "
            val dateLocal = tasks[i].dateTime.split('T')[0].toLocalDate()
            val (date, time) = tasks[i].dateTime.replace('T', ' ').split(" ")
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
            val numberOfDays = currentDate.daysUntil(dateLocal)
            val dueTag = when {
                numberOfDays == 0 -> DueTag.T
                numberOfDays > 0 -> DueTag.I
                else -> DueTag.O
            }
            val stringList = tasks[i].tasksList.first().windowed(44, 44, true)
            println(
                "| ${i + 1}$space| $date | $time |${Priority.valueOf(tasks[i].priority).color}|" +
                        "${dueTag.color}|${stringList[0].padEnd(44)}|"
            )
            for (i in 1..stringList.lastIndex) {
                println("$indent${ stringList[i].padEnd(44) }|")
            }
            if (tasks[i].tasksList.size > 1) {
                for (subTask in 1..tasks[i].tasksList.lastIndex) {
                    val tasksSplit = tasks[i].tasksList[subTask].windowed(44, 44, true)
                    for (part in tasksSplit.indices) {
                        println("$indent${ tasksSplit[part].padEnd(44) }|")
                    }
                }
            }
            println(horizontalLine)
        }
    }

    // Prompt the user to input the task number for further action
    private fun promptForTaskNumber(): Int {
        printTasks()
        if (tasks.isEmpty()) return 0
        while (true) {
            println("Input the task number (1-${tasks.size}):")
            val input = readln()
            try {
                tasks[input.toInt() - 1]
            } catch (e: Exception) {
                println("Invalid task number")
                continue
            }
            return input.toInt()
        }
    }

    // Delete the chosen task
    private fun deleteTask() {
        val taskNumber = promptForTaskNumber()
        if (taskNumber == 0) return
        tasks.removeAt(taskNumber - 1)
        println("The task is deleted")
    }

    // Edit the chosen task
    private fun editTask() {
        val taskNumber = promptForTaskNumber()
        if (taskNumber == 0) return
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            when (readln()) {
                "priority" -> {
                    tasks[taskNumber - 1].priority = promptForPriority()
                }
                "date" -> {
                    val oldDateTime = tasks[taskNumber - 1].dateTime
                    val newDate = promptForDate()
                    tasks[taskNumber - 1].dateTime = "${newDate}T${oldDateTime.split('T')[1]}"
                }
                "time" -> {
                    val oldDateTime = tasks[taskNumber - 1].dateTime
                    val newTime = promptForTime(oldDateTime)
                    tasks[taskNumber - 1].dateTime = "${oldDateTime.split('T')[0]}T${newTime.split('T')[1]}"
                }
                "task" -> {
                    val editedTask = promptForTasks()
                    tasks[taskNumber - 1].tasksList = editedTask
                }
                else -> {
                    println("Invalid field")
                    continue
                }
            }
            println("The task is changed")
            break
        }
    }

    // Menu prompts the user to input an action to be performed on the task list.
    fun chooseAction() {
        load()
        while (true) {
            println("Input an action (add, print, edit, delete, end):")
            when (readln().lowercase()) {
                "add" -> addTask()
                "print" -> printTasks()
                "edit" -> editTask()
                "delete" -> deleteTask()
                "end" -> {
                    println("Tasklist exiting!")
                    val json = taskListAdapter.toJson(tasks)
                    val jsonFile = File(currentDirectory, "tasklist.json")
                    FileWriter(jsonFile, Charset.defaultCharset()).use { it.write(json) }
                    exitProcess(0)
                }
                else -> println("The input action is invalid")
            }
        }
    }
}

fun main() {
    val taskList = TaskList()
    taskList.chooseAction()
}