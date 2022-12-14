package com.example.snake.client.net

import com.example.snake.client.Message.Message
import com.example.snake.proto.SnakesProto
import java.util.concurrent.Semaphore

/*если приводить аналогии, то этот класс реализует функционал wait/notify
он записывает клиентов в List, после приёма сообщений вызывает их метод update*/
open class Publisher {
    private val subscribers: MutableMap<SnakesProto.GameMessage.TypeCase, MutableList<Subscriber>> = HashMap()
    private val removeQueue = ArrayDeque<Subscriber>()

    private val queueLock = Semaphore(1)

    init {
        SnakesProto.GameMessage.TypeCase.values().forEach { case ->
            subscribers[case] = ArrayList()
        }
    }

    fun subscribe(sub: Subscriber, type: SnakesProto.GameMessage.TypeCase) {
        subscribers[type]?.add(sub)
    }

    fun unsubscribe(sub: Subscriber, type: SnakesProto.GameMessage.TypeCase) {
        if(queueLock.tryAcquire()) {
            subscribers[type]?.remove(sub)
        } else {
            removeQueue.add(sub)
        }
    }

    fun notifyMembers(data: Message, type: SnakesProto.GameMessage.TypeCase) {
        queueLock.acquire()

        subscribers[type]?.forEach { subscriber ->
            subscriber.update(data)
        }
        if (removeQueue.isNotEmpty()) {
            for (sub in removeQueue) {
                subscribers[type]?.remove(sub)
            }
            removeQueue.clear()
        }
        queueLock.release()

    }
}