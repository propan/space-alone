(ns space-alone.constants)

(def RAD_FACTOR (/ Math.PI 180))

(def MAX_SHIP_IMMUNITY 300)

;; screen dimensions
(def SCREEN_WIDTH 960)
(def SCREEN_HEIGHT 580)

(def LEFT_EDGE 5)
(def RIGHT_EDGE (- SCREEN_WIDTH LEFT_EDGE))

(def TOP_EDGE 5)
(def BOTTOM_EDGE (- SCREEN_HEIGHT TOP_EDGE))

;; movement constants
(def MAX_VELOCITY 6)
(def MAX_THRUST 2)

(def THRUST_DECLINE 0.3)

(def TURN_FACTOR 1.5)
(def ACCELERATION 0.01)

;; shooting constants
(def TIME_BETWEEN_SHOOTS 20)
(def BULLET_SPEED 15)
(def BULLET_ENERGY 35)

;; asteroids generation
(def ASTEROID_POINTS {1 [[9 6] [4 8] [2 11] [-2 9] [-3 9] [-5 10] [-8 6] [-11 2] [-9 -1] [-11 -3] [-10 -6] [-9 -8] [-2 -9] [1 -8] [2 -10] [4 -8] [10 -7]]
                      2 [[0 11] [-1 9] [-6 7] [-9 8] [-11 7] [-10 0] [-10 -4] [-7 -9] [-1 -10] [2 -8] [4 -10] [5 -9] [9 -8] [8 -7] [10 -4] [11 -2] [10 1] [7 7]]
                      3 [[2 8] [6 10] [10 -4] [5 -3] [6 -6] [0 -10] [-10 -4] [-10 6] [-4 9]]
                      4 [[10 -3] [5 -10] [-2 -8] [-5 -10] [-10 -5] [-8 1] [-8 10] [7 9]]})

(def MIN_TIME_BEFORE_ASTEROID 250)
(def MAX_TIME_BEFORE_ASTEROID 500)

(def ASTEROID_UNIT_SIZE 16)
(def ASTEROID_UNIT_REWARD 100)

;; controls
(def SPACE 32)

(def ARROW_LEFT 37)
(def ARROW_UP 38)
(def ARROW_RIGHT 39)
(def ARROW_DOWN 40)

(def N_KEY 78)

;; graphics

(def SHADOW_BLUR 9)

(def SHIP_COLOR "#0000FF")
(def TEXT_EFFECT_COLOR "#FFFF00")
(def WAVE_TEXT_COLOR "#FFFF00")
(def ASTEROID_PIECE_COLOR "#FF3030")
(def SHIELD_COLOR "#FFFFFF")
(def BULLET_COLOR "#FF0000")
(def PARTICLE_COLOR "#FFB236")
