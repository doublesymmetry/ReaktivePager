package com.doublesymmetry.library.rx

import com.badoo.reaktive.observable.*
import com.badoo.reaktive.subject.publish.PublishSubject

fun<T> observablePage(nextPage: (T?) -> Observable<T>, hasNext: (T) -> Boolean, trigger: Observable<Unit>): Observable<T> {
    fun next(fromPage: T?): Observable<T> {
        return nextPage(fromPage).map {
            if (!hasNext(it)) observableOf(it)
            else concat(observableOf(it), observableOfNever<T>().takeUntil(trigger), next(it))
        }.flatMap { it }
    }

    return next(null)
}

class Pager<T> {
    val page: Observable<T>
    val trigger = PublishSubject<Unit>()

    constructor(nextPage: (T?) -> Observable<T>, hasNext: (T) -> Boolean) {
        page = observablePage(nextPage, hasNext, trigger.asObservable())
    }

    fun next() {
        trigger.onNext(Unit)
    }
}