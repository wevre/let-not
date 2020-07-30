(ns wevre.let-not
  ^{:author "Mike Weaver"
    :see-also [["https://github.com/clojure/algo.monads"]
               ["https://brehaut.net/blog/2011/error_monads_revisited" "Error monads revisited"]]
    :doc "A monad for short-circuiting computations based on a `fail?` predicate."}
  (:require [clojure.algo.monads :refer [monad domonad]]))

(defn break-m
  "An extension to the `maybe-m` monad that in addition to checking for `nil?`
   checks if the computation fails using predicate `fail?`."
  [fail?]
  (monad [m-result identity
          m-bind   (fn [mv f] (if (or (nil? mv) (fail? mv)) mv (f mv)))]))

(defmacro let-not
  "Monad comprehension using `break-m` monad. 
   Use this to set up a chain of computations given as a `let` binding and a 
   final `return` expression. 
   If any computation returns `nil` or a value such that `(fail? val)` is true, 
   the whole computation will yield that value as well (i.e. short-circuit). 
   If none fail, the computation yields `return`. 
   Default `fail?` is `::break`. 
   "

  ([bindings return]
   `(let-not ::break ~bindings ~return))

  ([fail? bindings return]
   `(domonad (break-m ~fail?) ~bindings ~return)))
