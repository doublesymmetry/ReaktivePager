[![Bintray](https://img.shields.io/bintray/v/doublesymmetry/maven/reaktive-pager.svg)](https://bintray.com/doublesymmetry/maven/reaktive-pager/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache/2.0-blue.svg)](https://github.com/DoubleSymmetry/ReaktivePager/blob/master/LICENSE)

# ReaktivePager
ReaktivePager is a Kotlin multi-platform library that helps handle paginated results in a reactive way.

It is based on this [gist](https://gist.github.com/mttkay/24881a0ce986f6ec4b4d) from @mttkay.
It uses [Reaktive](https://github.com/badoo/Reaktive) by @badoo as the backing Rx library.

## Installation
Add Bintray repository into your root build.gradle file:

```groovy
repositories {
    maven {
        url  "https://dl.bintray.com/doublesymmetry/maven"
    }
}
```

Add publication:

```groovy
implementation 'com.doublesymmetry:reaktive-pager:<latest-version>'
```

## Usage
There's two ways to use this library: You can either create a `Pager` instance or use the `observablePage` method to create a paginated `Observable`.

#### 1) Creating a `Pager` instance
The `Pager` class contains an attribute `pager` that you can subscribe to. This will trigger `onNext` with the first page, and will continue to giving you the next page whenever you call `Pager.next()`. It will trigger `onComplete` when there is no more results to load. 

#### 2) Using the `observablePage` method to create an Observable
The method will return an `Observable` can subscribe to. This will trigger `onNext` with the first page, and will continue to giving you the next page whenever the given trigger is fired. It will trigger `onComplete` when there is no more results to load. 

## Examples

#### Using `Pager`
```kotlin
typealias Page = List<Int>

class SomePresenter {
    private val fetchNextTrigger = PublishSubject<Unit>()

    val pager: Pager<Page> = Pager(
        nextPage = {
            val last = it?.lastOrNull() ?: 0
            observableOf(listOf(last + 1, last + 2, last + 3))
        },
        hasNext = {
            val last = it.lastOrNull() ?: return@Pager true
            return@Pager last < 10 // arbitrary condition for the demo
        }
    )
}

// ...

pager.page.subscribe(onNext = { Log.d(it) })
// print [1, 2 ,3]

pager.next() // print [4, 5, 6]
pager.next() // print [7, 8, 9]
pager.next() // print [10, 11, 12]
```

#### Using `observablePage`

```kotlin
typealias Page = List<Int>

val nextPage: (Page?) -> Observable<Page> = nextPage@ {
    val last = it?.lastOrNull() ?: 0
    return@nextPage observableOf(listOf(last + 1, last + 2, last + 3))
}

val hasNext: (page: Page) -> Boolean = hasNext@ {
    val last = it.lastOrNull() ?: return@hasNext true
    return@hasNext last < 10 // arbitrary condition for the demo
}

val fetchNextTrigger = PublishSubject<Unit>()

val page = observablePage(nextPage, hasNext, fetchNextTrigger)

// ...

page.subscribe(onNext = { Log.d(it) })
// print [1, 2 ,3]

fetchNextTrigger.onNext() // print [4, 5, 6]
fetchNextTrigger.onNext() // print [7, 8, 9]
fetchNextTrigger.onNext() // print [10, 11, 12]
```

#### Coroutines interop
`Reaktive` [recommends to avoid using Ktor in Kotlin/Native multithreaded environment](https://github.com/badoo/Reaktive#coroutines-interop) until multithreaded coroutines, but if you really need to use it we've provided a helper `observableFromCoroutineUnsafe` that you can use in your `nextPage` declaration.

```kotlin
typealias Page = ...

class SomePresenter {
    private val page: Observable<Page> = {
        val nextPage: (Page?) -> Observable<Page> = {
            // Dispatcher should be defined per platform
            observableFromCoroutineUnsafe(Dispatcher) {
                return@observableFromCoroutineUnsafe ...
            }
        }

        val hasNext: (page: Page?) -> Boolean = { it?.hasMore == true }

        observablePage(nextPage, hasNext, trigger)
    }()
}

// ...

page.subscribe(onNext = { Log.d(it) })
```
