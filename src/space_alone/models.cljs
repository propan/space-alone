(ns space-alone.models
  (:require [space-alone.constants :as C]
            [space-alone.utils :as u]))

(defrecord Asteroid [x y vX vY energy size type rotate rotation rotation-speed])

(defrecord Bullet [x y vX vY energy radius])

(deftype CachedImage [width height data])

(defrecord ObjectPiece [x y lx ly rx ry size vX vY rotate rotation rotation-speed color lifespan ticks-left])

(defrecord Particle [x y vX vY radius lifespan ticks-left])

(defrecord Ship [x y vX vY thrust rotation rotate accelerate shoot next-shoot radius immunity])

(defrecord TextEffect [x y text scale scale-speed color lifespan ticks-left])

(defrecord ThrustEffect [x y rotation lifespan ticks-left])

(defrecord GameScreen [background-image asteroids bullets ship effects next-asteroid lives score wave asteroids-left])

(defrecord GameOverScreen [background-image asteroids bullets effects score])

(defrecord WelcomeScreen [background-image asteroids])

(defn random-rotation
  []
  (let [direction (Math/random)]
    (cond
     (< direction 0.33) :left
     (< direction 0.66) :right
     :else              :none)))

(defn random-speed
  [size direction]
  (case size
    4 (* direction (u/random-float 0.01 0.3))
    3 (* direction (u/random-float 0.1  0.5))
    2 (* direction (u/random-float 0.2  0.7))
    1 (* direction (u/random-float 0.3  0.9))))

(defn random-direction
  []
  (if (< (Math/random) 0.5) -1 1))

(defn x-dir
  [x]
  (cond
   (< x C/LEFT_EDGE) 1
   (> x C/RIGHT_EDGE) -1
   :else (random-direction)))

(defn y-dir
  [y]
  (cond
   (< y C/TOP_EDGE) -1
   (> y C/BOTTOM_EDGE) 1
   :else (random-direction)))

(defn asteroid
  [x y size]
  (Asteroid. x y (random-speed size (x-dir x))
                 (random-speed size (y-dir y))
                 (* size 20)
                 size
                 (u/random-int 1 4)
                 (random-rotation)
                 0
                 (u/random-float 0.1 0.6)))

(defn asteroid-piece
  [x y lx ly rx ry size rotation]
  (let [lifespan (u/random-int 10 20)]
    (ObjectPiece. x y lx ly rx ry size
                  (* (random-direction) (u/random-float 0.5 1.0))
                  (* (random-direction) (u/random-float 0.5 1.0))
                  (random-rotation) rotation
                  (u/random-float 0.1 0.5)
                  C/ASTEROID_PIECE_COLOR
                  lifespan
                  lifespan)))

(defn bullet
  [x y rotation]
  (let [vX (* C/BULLET_SPEED (Math/sin (* rotation (- C/RAD_FACTOR))))
        vY (* C/BULLET_SPEED (Math/cos (* rotation (- C/RAD_FACTOR))))]
    (Bullet. (- x vX) (- y vY) vX vY C/BULLET_ENERGY 5)))

(defn ship-piece
  [x y lx ly rx ry vX vY rotation]
  (let [lifespan (u/random-int 40 80)]
    (ObjectPiece. x y lx ly rx ry 1
                  (+ (* 0.1 vX) (u/random-float 0.5 1.5))
                  (+ (* 0.1 vY) (u/random-float 0.5 1.5))
                  (random-rotation) rotation
                  (u/random-float 0.2 0.5)
                  C/SHIP_COLOR
                  lifespan
                  lifespan)))

(defn particle
  [x y]
  (let [rotation (u/random-int 0 360)
        vX       (* (u/random-float C/MIN_PARTICLE_SPEED C/MAX_PARTICLE_SPEED) (Math/sin (* rotation (- C/RAD_FACTOR))))
        vY       (* (u/random-float C/MIN_PARTICLE_SPEED C/MAX_PARTICLE_SPEED) (Math/cos (* rotation (- C/RAD_FACTOR))))
        lifespan (u/random-int 5 25)]
    (Particle. x y vX vY (u/random-int 1 5) lifespan lifespan)))

(defn ship
  [x y immunity]
  (Ship. x y 0 0 0 0 :none false false 0 15 immunity))

(defn score-text
  [text x y]
  (TextEffect. x y text 0.7 0.05 C/TEXT_EFFECT_COLOR 30 30))

(defn thrust-effect
  [x y rotation]
  (ThrustEffect. x y rotation 10 10))

(defn wave-text
  [wave-number]
  (TextEffect. (/ C/SCREEN_WIDTH 2)
               (+ (/ C/SCREEN_HEIGHT 2) 40)
               (str "WAVE " wave-number)
               3.5 0.15
               C/WAVE_TEXT_COLOR 50 50))

(defn game-screen
  []
  (GameScreen. (u/image "resources/images/background.jpg")
               [] []
               (ship (/ C/SCREEN_WIDTH 2)
                     (/ C/SCREEN_HEIGHT 2)
                     0)
               [(wave-text 1)]
               (u/random-int C/MIN_TIME_BEFORE_ASTEROID
                             C/MAX_TIME_BEFORE_ASTEROID)
               3 0 1 1))

(defn game-over-screen
  [{:keys [background-image asteroids bullets effects score]}]
  (GameOverScreen. background-image asteroids bullets effects score))

(defn welcome-screen
  []
  (WelcomeScreen. (u/image "resources/images/background.jpg")
                  (repeatedly 5 #(asteroid (u/random-int C/LEFT_EDGE C/RIGHT_EDGE)
                                           (u/random-int C/TOP_EDGE C/BOTTOM_EDGE) 4))))
