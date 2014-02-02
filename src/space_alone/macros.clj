(ns space-alone.macros)

(defmacro with-context
  [bindings & body]
  (cond
   (= (count bindings) 0) `(do ~@body)
   (symbol? (bindings 0)) (let [context (bindings 0)]
                            `(let ~(subvec bindings 0 2)
                               (.save ~context)
                               (with-context ~(subvec bindings 2) ~@body)
                               (.restore ~context)))
   :else (throw (IllegalArgumentException.
                 "with-open only allows Symbols in bindings"))))
