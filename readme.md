# let-not

Monad comprehension for a chain of computations, specified with a `fail?`
predicate, a `let` binding, and a final `return` expression. It extends the
`maybe-m` monad such that if any computation returns `nil` _or_ a value such
that `(fail? val)` is true, the whole computation will yield that value (i.e.
short-circuit). If none fail, the computation returns `result`. The default
`fail?` predicate is `::break`, implying a typical situation where computations
are collecting results in a map, and the presence of the key `::break` will
indicate failure.

# How to install

deps.edn

    wevre/let-not {:mvn/version "0.0.1"}

project.clj

    [wevre/let-not "0.0.1"]

# How to use

```
(ns my-namespace
  (:require [wevre.let-not :refer [let-not]]))

(defn something-with [x]
  (if (looks-okay x)
    {:status "everything looks good"}
    {:error-state "we failed"}))

(defn check-a-bunch-of-things [input]
  (let-not :error-state
    [a (something-with input)
     b (something-else-with a)]
    (final-result-with b)))
```

# Why do I need this?

I created `let-not` because one of my favorite constructs in imperative
languages (descendants of C) was a `do-while(0)` loop for short-circuiting. I
would deal with all my 'unhappy path' items at the beginning and bail out early
if anything went wrong (cf. Swift's `guard` statement).

```
do {
   <something that might break>
   <next thing that might break>
   <everything is good to go, do your thing>
   return;
} while (0);
<handle the breaking issue>
```

This can also be wrapped in an exception if needed, although I personally try to
reduce my dependency on those.

```
try do {
   <do breaking/exception stuff>
   <do your thing>
   return;
} while(0) catch (Exception e) {
   <handle the exception>
}
<handle the breaking issue>
```

Or you could catch the exception inside the `do-while` and then break from
there...lots of variations.

I'm programming in Clojure and I still find myself in situations where I want to
use this approach: I need to check that a bunch of stuff is okay and, if it is,
do the final thing. My choices are (a) nested `if`s (or `if-let`s or
`when-let`s) or `cond`; or (b) exceptions.

```
(if <first thing is okay>
  (if <second thing is okay>
    (if <third thing is okay>
      ...   ;; pyramid of doom
        <do your thing>
        <handle nth error>)
      ...
      <handle 2nd error>)
   <handle 1st error>)

(try
  (do
    <thing that might throw>
    <another thing that might throw>
    ...
    <do your thing>)
  (catch ...)

(cond
  <fails first test> <handle 1st error>
  <fails second test> <handle 2nd error>
  ...
  <one final check> <do your thing>
  :else <handle something you didn't think about>)
```

In the above code, "handle error" might mean returing nil or some other value
that indicates failure, it might mean throwing an exception. In all three
approaches, which are already cumbersome, it gets messy if you need to bind
intermediate values along the way. You have to intersperse your code with `let`
bindings which turn pyramids of doom into _super_ pyramids of doom.

# Where would I use it?

`let-not` cleans up code where you are testing your way through a list of
conditions, and might need to keep track of intermediate values along the way. I
think there are two main uses:

 1. As a replacement for exceptions.

 2. As a replacement for `cond` or nested `if`s (especially if you need to keep
    track of intermediate values).

# An example

Here is an example of using it to re-write the sample from
[clojure/tools.cli](https://github.com/clojure/tools.cli), replacing the `cond`
used in the `validate-args` function with an approach using `let-not`. This
maybe isn't the most scintillating of examples. We don't really need to keep
track of any intermediate values. But it still demonstrates nicely the use of
`let-not`. When I find a better practical example, I'll replace it.

## Original clojure/tools.cli example code

```
(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"start" "stop" "status"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))
```

## Replace `cond` with `let-not`

```
(ns myns
  (:require [wevre.let-not :refer [let-not]))

(defn validate-args-let-not
  "Validate command line arguments with `let-not`. At each step, if a condition
  fails, return a map with included :exit-message key, otherwise return the prior
  step's result."
  [args]
  (let-not :exit-message
    [{:keys [options arguments errors summary] :as res} (parse-opts args cli-options)
     res (if (:help options) {:exit-message (usage summary) :ok? true} res)
     res (if errors {:exit-message (error-msg errors)} res)
     res (if (not= 1 (count arguments)) {:exit-message (usage summary)} res)
     res (if (nil? (#{"start" "stop" "status"} (first arguments))) {:exit-message (usage summary)} res)]
    {:action (first arguments) :options options}))
```

# License

Copyright 2020 Mike Weaver

Licensed under the Eclipse Public License 2.0, see 'license.txt'.
