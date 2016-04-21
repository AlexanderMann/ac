# ac
```
      _                   __    ___
 ___ /|\ = ~ - ~   -   ~ / _\  / __)
|___|-o-| = - ~ - -   ~ /    \( (__
 ||  \|/ = ~ ~ - ~   .  \_/\_/ \___)
 /\
```

A Clojure library for running functions as a separate JVM.

## Usage

```clojure
(ns demo
  (:require [me.alxmnn.ac :as ac]))

; First we declare some function where we care about the
; side effects, but know there's a chance that it could run
; out of memory. A great example is reading TEXT fields from
; a PostgreSQL DB. Those fields are potentially 4GB in size,
; which can be too much to safely handle.
(defn use-all-the-memory!
  [period1 period2 period3]
  (apply str (repeat (str period1 period2 period3))))

; We create a standalone process which will run our
; fully-qualified fn from above in a separate JVM, protecting
; our current JVM from OOM.
(def p (ac/create-standalone-process 'demo/use-all-the-memory!
                                     "."
                                     "."
                                     "."))

; Let the process exit
(ac/wait-for-exit p)

; And we are still able to continue running our JVM even though
; we just "executed" code that causes an OOM.
(println (ac/error-message p))
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
