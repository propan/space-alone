(ns space-alone.utils)

(defn random-int
  [min max]
  (Math/floor (+ min (* (Math/random) (- max min -1)))))

(defn random-float
  [min max]
  (+ min (* (Math/random) (- max min))))
