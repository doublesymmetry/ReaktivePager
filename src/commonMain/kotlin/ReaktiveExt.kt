package com.doublesymmetry.library.rx

import com.badoo.reaktive.completable.CompletableCallbacks
import com.badoo.reaktive.coroutinesinterop.asDisposable
import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.*
import com.badoo.reaktive.utils.atomic.AtomicReference
import com.badoo.reaktive.utils.atomic.getAndSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T> Observable<T>.asObservable(): Observable<T> {
    return observableUnsafe {
        this.subscribe(it)
    }
}

fun <T> observableFromCoroutineUnsafe(mainContext: CoroutineContext, block: suspend CoroutineScope.() -> T): Observable<T> =
    observable { emitter ->
        GlobalScope
            .launch(mainContext) {
                try {
                    emitter.onNext(block())
                } catch (e: Throwable) {
                    emitter.onError(e)
                } finally {
                    emitter.onComplete()
                }
            }
            .asDisposable()
            .also(emitter::setDisposable)
    }

fun <T> Observable<T>.withPrevious(): Observable<Pair<T?, T>> =
    observable { emitter ->
        val previousRef = AtomicReference<T?>(null)

        subscribe(
            object : ObservableObserver<T>, CompletableCallbacks by emitter {
                override fun onSubscribe(disposable: Disposable) {
                    emitter.setDisposable(disposable)
                }

                override fun onNext(value: T) {
                    emitter.onNext(previousRef.getAndSet(value) to value)
                }
            }
        )
    }
