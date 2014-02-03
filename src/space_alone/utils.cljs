(ns space-alone.utils)

(defn random-int
  [min max]
  (Math/floor (+ min (* (Math/random) (- max min -1)))))

(defn random-float
  [min max]
  (+ min (* (Math/random) (- max min))))

(defn distance
  [x1 y1 x2 y2]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))
