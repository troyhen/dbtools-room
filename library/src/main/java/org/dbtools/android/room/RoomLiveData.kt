@file:Suppress("MemberVisibilityCanPrivate")

package org.dbtools.android.room

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.InvalidationTracker
import android.arch.persistence.room.RoomDatabase
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.CoroutineContext

/**
 * This class creates a LiveData that will invalidate/update when there are changes to specific tables of a database.
 * This is useful when simple LiveData from Room Dao objects is not enough.
 *
 * Example:
 *
 * // CREATE the live data
 * val liveData = mainDatabase.toLiveData("individual", "foo") {
 *     // do lots of work to pull data from different resources and possibly perform some custom data manipulation
 *     // this code will get executed anytime there are changes on the "individual" table
 *     arrayOf("a", "b", "c")
 * }
 *
 * // OBSERVE
 * liveData.observe(this@MainActivity, Observer { data ->
 *     data ?: return@Observer
 *
 *     // show data ("a", "b", "c")
 * })
 *
 * // CHANGE - This will cause the liveData to be invalidated/re-computed/fired to the observer
 * individualDao().updateName(id, "Bob")
 *
 *
 * (Alternative) Multiple Databases:
 * val liveData = mainDatabase.toLiveData(listOf(database1.tableChangeReferences("downloads"), database2.tableChangeReferences("collections"))) {
 *     arrayOf("a", "b", "c")
 * }
 */
@Suppress("unused")
object RoomLiveData {
    /**
     * Return data retrieved via block parameter as LiveData
     *
     * @param coroutineContext Thread in which block is executed on
     * @param block Function that is executed to get data
     *
     * @return LiveData<T>
     */
    fun <T> toLiveData(coroutineContext: CoroutineContext = CommonPool, block: suspend () -> T): LiveData<T> {
        return toLiveDataInternal(null, coroutineContext, block)
    }

    /**
     * Return data retrieved via block parameter as LiveData
     *
     * @param tableChangeReferences Tables that will cause this LiveData to be triggered
     * @param coroutineContext Thread in which block is executed on
     * @param block Function that is executed to get data
     *
     * @return LiveData<T>
     */
    fun <T> toLiveData(tableChangeReferences: List<TableChangeReference>, coroutineContext: CoroutineContext = CommonPool, block: suspend () -> T): LiveData<T> {
        return toLiveDataInternal(tableChangeReferences, coroutineContext, block)
    }

    private fun <T> toLiveDataInternal(
        tableChangeReferences: List<TableChangeReference>?,
        coroutineContext: CoroutineContext,
        block: suspend () -> T
    ): LiveData<T> {
        return object : LiveData<T>() {
            private val computing = AtomicBoolean(false)
            private val invalid = AtomicBoolean(true)
            private lateinit var job: Job
            private var observerList = mutableListOf<Pair<TableChangeReference, InvalidationTracker.Observer>>()

            private fun addObserver(tableChangeReference: TableChangeReference?) {
                tableChangeReference ?: return

                // Create Observer
                val tableNames = tableChangeReference.tableNames
                val observer = object : InvalidationTracker.Observer(tableNames) {
                    override fun onInvalidated(tables: MutableSet<String>) {
                        onTableChange()
                    }
                }

                // Create a Weak Observer
                val invalidationTracker = tableChangeReference.database.invalidationTracker
                val weakObserver = WeakObserver(invalidationTracker, observer, tableNames)

                // Observe
                invalidationTracker.addObserver(weakObserver)

                observerList.add(Pair(tableChangeReference, observer))
            }

            override fun onActive() {
                job = Job()

                tableChangeReferences?.forEach { tableChangeManager -> addObserver(tableChangeManager) }
                getData()
            }

            protected fun finalize() {
                job.cancel()
            }

            private fun onTableChange() {
                job.cancel()
                job = Job()
                invalid.set(true)
                getData()
            }

            private fun getData() {
                launch(coroutineContext, parent = job) {
                    var computed: Boolean
                    do {
                        computed = false
                        if (computing.compareAndSet(false, true)) {
                            try {
                                var value: T? = null
//                                checkChangeTs()
                                while (invalid.compareAndSet(true, false)) {
                                    computed = true
                                    value = block()
//                                    updateChangeTs()
                                }
                                if (computed) {
                                    postValue(value)
                                }
                            } finally {
                                // release compute lock
                                computing.set(false)
                            }
                        }

                        // Why all the ugly code: (Copied from ComputableLiveData)
                        // check invalid after releasing compute lock to avoid the following scenario.
                        // Thread A runs compute()
                        // Thread A checks invalid, it is false
                        // Main thread sets invalid to true
                        // Thread B runs, fails to acquire compute lock and skips
                        // Thread A releases compute lock
                        // We've left invalid in set state. The check below recovers.
                    } while (computed && invalid.get())
                }
            }
        }
    }

    /**
     * An Observer wrapper that keeps a weak reference to the given object.
     *
     * This class with automatically unsubscribe when the wrapped observer goes out of memory.
     */
    internal class WeakObserver(
        private val invalidationTracker: InvalidationTracker,
        delegate: InvalidationTracker.Observer,
        tableNames: Array<out String>
    ) : InvalidationTracker.Observer(tableNames) {
        private val delegateRef: WeakReference<InvalidationTracker.Observer> = WeakReference(delegate)

        override fun onInvalidated(tables: Set<String>) {
            val observer = delegateRef.get()
            if (observer == null) {
                invalidationTracker.removeObserver(this)
            } else {
                observer.onInvalidated(tables)
            }
        }
    }
}

open class TableChangeReference(val database: RoomDatabase, val tableNames: Array<out String>)

fun RoomDatabase.tableChangeReferences(vararg tableNames: String): TableChangeReference {
    return TableChangeReference(this, tableNames)
}

fun <T> RoomDatabase.toLiveData(vararg tableNames: String, coroutineContext: CoroutineContext = CommonPool, block: suspend () -> T): LiveData<T> {
    return RoomLiveData.toLiveData(listOf(TableChangeReference(this, tableNames)), coroutineContext) { block() }
}
