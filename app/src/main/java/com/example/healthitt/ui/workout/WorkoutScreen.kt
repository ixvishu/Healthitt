package com.example.healthitt.ui.workout

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthitt.ui.theme.*
import kotlinx.coroutines.delay

data class Workout(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val difficulty: String, 
    val description: String,
    val position: String,
    val sets: String,
    val reps: String,
    val caloriesBurned: Int,
    val imageName: String,
    val durationSeconds: Int? = null
)

val workoutData = listOf(
    // CHEST (20)
    Workout(1, "Bench Press", "Chest", "Intermediate", "Core mass builder.", "Lie on bench, lower bar to mid-chest, push up.", "4 Sets", "10 Reps", 120, "bench_press"),
    Workout(2, "Incline DB Press", "Chest", "Intermediate", "Upper chest focus.", "Set bench to 45 degrees, press dumbbells upward.", "3 Sets", "12 Reps", 110, "incline_db_press"),
    Workout(3, "Chest Flys", "Chest", "Beginner", "Chest definition.", "Lie on bench, open arms wide, squeeze at top.", "3 Sets", "15 Reps", 80, "chest_flys"),
    Workout(4, "Pushups", "Chest", "Beginner", "Bodyweight builder.", "Plank position, lower chest to floor, push back up.", "3 Sets", "20 Reps", 70, "pushups"),
    Workout(5, "Cable Crossover", "Chest", "Intermediate", "Inner chest focus.", "Pull cables from high to low in front of chest.", "3 Sets", "15 Reps", 85, "cable_crossover"),
    Workout(6, "Dips", "Chest", "Advanced", "Lower chest mass.", "Suspend on bars, lean forward, lower and push up.", "3 Sets", "12 Reps", 95, "chest_dips"),
    Workout(7, "Decline Press", "Chest", "Intermediate", "Lower pectoral focus.", "Lie on decline bench, press weight upward.", "3 Sets", "10 Reps", 115, "decline_press"),
    Workout(8, "Pec Deck", "Chest", "Beginner", "Controlled isolation.", "Sit in machine, bring handles together in front.", "3 Sets", "15 Reps", 75, "pec_deck"),
    Workout(9, "DB Pullover", "Chest", "Intermediate", "Serratus and chest.", "Lie across bench, lower dumbbell behind head.", "3 Sets", "12 Reps", 90, "db_pullover"),
    Workout(10, "Landmine Press", "Chest", "Beginner", "Upper chest stability.", "Push end of barbell upward from floor.", "3 Sets", "12 Reps", 100, "landmine_press"),
    Workout(11, "Chest Press Machine", "Chest", "Beginner", "Safe mass builder.", "Sit in machine and push handles forward.", "3 Sets", "12 Reps", 80, "chest_press_machine"),
    Workout(12, "Weighted Pushups", "Chest", "Advanced", "Increased resistance.", "Pushups with weight plate on back.", "3 Sets", "12 Reps", 90, "weighted_pushups"),
    Workout(13, "Svend Press", "Chest", "Intermediate", "Inner chest squeeze.", "Press two plates together in front of chest.", "3 Sets", "15 Reps", 60, "svend_press"),
    Workout(14, "Around the Worlds", "Chest", "Intermediate", "Full range movement.", "Move dumbbells in circle above chest.", "3 Sets", "12 Reps", 70, "around_the_worlds"),
    Workout(15, "Medicine Ball Pushups", "Chest", "Advanced", "Stability focus.", "Pushups with hands on medicine ball.", "3 Sets", "12 Reps", 85, "med_ball_pushups"),
    Workout(16, "Incline Flys", "Chest", "Intermediate", "Upper chest stretch.", "Fly movement on incline bench.", "3 Sets", "12 Reps", 75, "incline_flys"),
    Workout(17, "Cable Press", "Chest", "Intermediate", "Functional strength.", "Press cables forward from standing position.", "3 Sets", "15 Reps", 90, "cable_press"),
    Workout(18, "Floor Press", "Chest", "Intermediate", "Tricep/Chest power.", "Lie on floor, press bar up, elbows touch ground.", "4 Sets", "8 Reps", 100, "floor_press"),
    Workout(19, "Spiderman Pushups", "Chest", "Advanced", "Oblique/Chest combo.", "Knee to elbow during pushup downward phase.", "3 Sets", "12 Reps", 95, "spiderman_pushups"),
    Workout(20, "Single Arm DB Press", "Chest", "Intermediate", "Core/Chest balance.", "Press one dumbbell while lying on bench.", "3 Sets", "10 Reps", 80, "single_arm_db_press"),

    // BACK (20)
    Workout(21, "Deadlifts", "Back", "Advanced", "Full body power.", "Hinge at hips, lift bar from floor to standing.", "4 Sets", "8 Reps", 200, "deadlifts"),
    Workout(22, "Pullups", "Back", "Advanced", "Upper body width.", "Hang from bar, pull chin above bar.", "3 Sets", "10 Reps", 110, "pullups"),
    Workout(23, "Bent Over Rows", "Back", "Intermediate", "Mid-back thickness.", "Hinge forward, pull bar to lower stomach.", "4 Sets", "10 Reps", 130, "bent_over_rows"),
    Workout(24, "Lat Pulldown", "Back", "Beginner", "Lat isolation.", "Sit at machine, pull bar to upper chest.", "3 Sets", "12 Reps", 90, "lat_pulldown"),
    Workout(25, "Seated Cable Row", "Back", "Beginner", "Rhomboid focus.", "Pull cable handle to waist while sitting.", "3 Sets", "12 Reps", 95, "seated_cable_row"),
    Workout(26, "T-Bar Row", "Back", "Intermediate", "Back thickness.", "Pull weighted end of bar between legs.", "3 Sets", "10 Reps", 125, "t_bar_row"),
    Workout(27, "One Arm DB Row", "Back", "Beginner", "Back symmetry.", "Row dumbbell to hip leaning on bench.", "3 Sets", "12 Reps", 85, "one_arm_db_row"),
    Workout(28, "Face Pulls", "Back", "Beginner", "Rear delts focus.", "Pull rope towards face, ends apart.", "3 Sets", "15 Reps", 60, "face_pulls"),
    Workout(29, "Hyperextensions", "Back", "Beginner", "Lower back strength.", "Lower and raise torso on specialized bench.", "3 Sets", "15 Reps", 70, "hyperextensions"),
    Workout(30, "Single Arm Lat Pull", "Back", "Intermediate", "Unilateral lat work.", "Pull cable down to side with one arm.", "3 Sets", "12 Reps", 80, "single_arm_lat_pull"),
    Workout(31, "Good Mornings", "Back", "Advanced", "Lower back/Hamstring.", "Hinge with bar on shoulders.", "3 Sets", "10 Reps", 90, "good_mornings"),
    Workout(32, "Superman", "Back", "Beginner", "Erector spinae.", "Lift arms and legs while lying on stomach.", "3 Sets", "20s Hold", 40, "superman", durationSeconds = 20),
    Workout(33, "Inverted Row", "Back", "Beginner", "Functional pull.", "Row bodyweight while hanging under bar.", "3 Sets", "12 Reps", 75, "inverted_row"),
    Workout(34, "Pendlay Row", "Back", "Advanced", "Explosive power.", "Strict row from floor for each rep.", "4 Sets", "8 Reps", 140, "pendlay_row"),
    Workout(35, "V-Bar Pulldown", "Back", "Beginner", "Close grip focus.", "Use V-handle for lat pulldown.", "3 Sets", "12 Reps", 85, "v_bar_pulldown"),
    Workout(36, "Renegade Row", "Back", "Advanced", "Core/Back combo.", "Row dumbbells in plank position.", "3 Sets", "10 Reps", 100, "renegade_row"),
    Workout(37, "Pull-overs (Cable)", "Back", "Intermediate", "Straight arm pull.", "Pull cable down with straight arms.", "3 Sets", "15 Reps", 70, "cable_pullovers"),
    Workout(38, "Rack Pulls", "Back", "Intermediate", "Upper back/Traps.", "Deadlift from knee height in rack.", "3 Sets", "8 Reps", 150, "rack_pulls"),
    Workout(39, "Bird Dog", "Back", "Beginner", "Core stability back.", "Opposite arm and leg extension on knees.", "3 Sets", "12 Reps", 30, "bird_dog"),
    Workout(40, "Chin-ups", "Back", "Intermediate", "Lats/Bicep combo.", "Pullup with palms facing towards you.", "3 Sets", "10 Reps", 105, "chin_ups"),

    // SHOULDERS (20)
    Workout(41, "Overhead Press", "Shoulders", "Intermediate", "Shoulder mass.", "Press barbell from chest to above head.", "4 Sets", "8 Reps", 110, "overhead_press"),
    Workout(42, "Lateral Raises", "Shoulders", "Beginner", "Shoulder width.", "Lift dumbbells out to sides until parallel.", "3 Sets", "15 Reps", 60, "lateral_raises"),
    Workout(43, "Arnold Press", "Shoulders", "Intermediate", "Full shoulder press.", "Rotate palms while pressing overhead.", "3 Sets", "12 Reps", 100, "arnold_press"),
    Workout(44, "Front Raises", "Shoulders", "Beginner", "Front delt focus.", "Lift dumbbells straight in front of body.", "3 Sets", "15 Reps", 55, "front_raises"),
    Workout(45, "Upright Rows", "Shoulders", "Intermediate", "Traps and delts.", "Pull barbell straight up to chin height.", "3 Sets", "12 Reps", 80, "upright_rows"),
    Workout(46, "Reverse Flys", "Shoulders", "Beginner", "Rear delt isolation.", "Hinge forward, lift dumbbells to sides.", "3 Sets", "15 Reps", 50, "reverse_flys"),
    Workout(47, "DB Shrugs", "Shoulders", "Beginner", "Trap thickness.", "Hold heavy dumbbells, shrug to ears.", "4 Sets", "15 Reps", 70, "db_shrugs"),
    Workout(48, "Push Press", "Shoulders", "Advanced", "Explosive overhead.", "Use legs to help drive barbell overhead.", "3 Sets", "8 Reps", 130, "push_press"),
    Workout(49, "Cable Lateral Raise", "Shoulders", "Intermediate", "Side delt tension.", "Lift cable across body to the side.", "3 Sets", "15 Reps", 65, "cable_lateral_raise"),
    Workout(50, "Military Press", "Shoulders", "Intermediate", "Standing power.", "Strict overhead press while standing.", "3 Sets", "10 Reps", 115, "military_press"),
    Workout(51, "Clean and Press", "Shoulders", "Advanced", "Full body explosive.", "Lift bar from floor to chest, then press.", "3 Sets", "8 Reps", 160, "clean_and_press"),
    Workout(52, "Handstand Pushups", "Shoulders", "Advanced", "Bodyweight power.", "Pushup while in handstand against wall.", "3 Sets", "8 Reps", 140, "handstand_pushups"),
    Workout(53, "Face Pulls", "Shoulders", "Beginner", "Postural health.", "Pull rope to face, squeezing rear delts.", "3 Sets", "15 Reps", 60, "shoulder_face_pulls"),
    Workout(54, "Bus Drivers", "Shoulders", "Beginner", "Front delt endurance.", "Hold plate in front and rotate like wheel.", "3 Sets", "45s", 50, "bus_drivers", durationSeconds = 45),
    Workout(55, "Snatch Grip High Pull", "Shoulders", "Advanced", "Trap/Shoulder power.", "Explosive pull to upper chest.", "3 Sets", "10 Reps", 130, "snatch_grip_pull"),
    Workout(56, "Cuban Press", "Shoulders", "Intermediate", "Rotator cuff focus.", "Upright row, rotate, press overhead.", "3 Sets", "12 Reps", 75, "cuban_press"),
    Workout(57, "Scaption", "Shoulders", "Beginner", "Lower trap/Shoulder.", "Raise dumbbells in 'V' shape to front.", "3 Sets", "15 Reps", 45, "scaption"),
    Workout(58, "Barbell Shrugs", "Shoulders", "Beginner", "Upper traps.", "Shrug barbell in front of hips.", "4 Sets", "12 Reps", 80, "barbell_shrugs"),
    Workout(59, "Single Arm DB Shoulder Press", "Shoulders", "Intermediate", "Core stability press.", "Press one dumbbell while standing.", "3 Sets", "10 Reps", 85, "single_arm_db_shoulder_press"),
    Workout(60, "Lu Raises", "Shoulders", "Intermediate", "Side delt focus.", "Full range lateral raise above head.", "3 Sets", "12 Reps", 70, "lu_raises"),

    // BICEPS (20)
    Workout(61, "Barbell Curls", "Biceps", "Beginner", "Overall mass.", "Curl barbell towards shoulders.", "3 Sets", "12 Reps", 70, "barbell_curls"),
    Workout(62, "Hammer Curls", "Biceps", "Beginner", "Bicep thickness.", "Curl dumbbells with palms facing in.", "3 Sets", "12 Reps", 65, "hammer_curls"),
    Workout(63, "Preacher Curls", "Biceps", "Intermediate", "Lower bicep peak.", "Curl bar while arms rest on angled pad.", "3 Sets", "12 Reps", 60, "preacher_curls"),
    Workout(64, "Concentration Curl", "Biceps", "Beginner", "Peak isolation.", "Curl dumbbell while sitting, arm on thigh.", "3 Sets", "12 Reps", 55, "concentration_curl"),
    Workout(65, "Incline DB Curls", "Biceps", "Intermediate", "Long head stretch.", "Curl dumbbells on incline bench.", "3 Sets", "12 Reps", 65, "incline_db_curls"),
    Workout(66, "Cable Curls", "Biceps", "Beginner", "Constant tension.", "Curl cable bar towards chest.", "3 Sets", "15 Reps", 60, "cable_curls"),
    Workout(67, "EZ Bar Curls", "Biceps", "Beginner", "Wrist-friendly curls.", "Use EZ bar for comfortable grip curls.", "3 Sets", "12 Reps", 70, "ez_bar_curls"),
    Workout(68, "Spider Curls", "Biceps", "Intermediate", "Strict isolation.", "Lie chest-down on incline bench, curl bar.", "3 Sets", "12 Reps", 65, "spider_curls"),
    Workout(69, "Zottman Curls", "Biceps", "Intermediate", "Bicep and forearm.", "Curl up normally, rotate palms down for low.", "3 Sets", "12 Reps", 75, "zottman_curls"),
    Workout(70, "Bicep Chin-ups", "Biceps", "Intermediate", "Bodyweight biceps.", "Pull chin above bar with palms facing you.", "3 Sets", "10 Reps", 100, "bicep_chin_ups"),
    Workout(71, "Reverse Barbell Curls", "Biceps", "Intermediate", "Brachioradialis focus.", "Curl bar with palms facing down.", "3 Sets", "12 Reps", 60, "reverse_barbell_curls"),
    Workout(72, "Drag Curls", "Biceps", "Intermediate", "Peak focus.", "Pull bar up along the front of your torso.", "3 Sets", "12 Reps", 65, "drag_curls"),
    Workout(73, "Single Arm Cable Curl", "Biceps", "Beginner", "Isolation focus.", "Curl cable with one hand.", "3 Sets", "15 Reps", 50, "single_arm_cable_curl"),
    Workout(74, "High Cable Curls", "Biceps", "Intermediate", "Short head focus.", "Curl cables from shoulder height sideways.", "3 Sets", "12 Reps", 55, "high_cable_curls"),
    Workout(75, "Hammer Rope Curl", "Biceps", "Beginner", "Thickness focus.", "Curl cable rope with palms in.", "3 Sets", "15 Reps", 60, "hammer_rope_curl"),
    Workout(76, "Plate Curls", "Biceps", "Beginner", "Grip/Bicep combo.", "Hold plate with fingers, curl to chin.", "3 Sets", "12 Reps", 45, "plate_curls"),
    Workout(77, "Waiters Curls", "Biceps", "Intermediate", "Outer peak focus.", "Hold dumbbell vertical with palms up, curl.", "3 Sets", "12 Reps", 50, "waiters_curls"),
    Workout(78, "21s Curls", "Biceps", "Advanced", "Muscle burnout.", "7 low, 7 high, 7 full curls.", "3 Sets", "21 Reps", 90, "curls_21s"),
    Workout(79, "Resistance Band Curl", "Biceps", "Beginner", "Home workout focus.", "Stand on band and curl handles.", "3 Sets", "20 Reps", 40, "resistance_band_curl"),
    Workout(80, "Dumbbell Twist Curl", "Biceps", "Beginner", "Peak contraction.", "Rotate palms up as you curl dumbbells.", "3 Sets", "12 Reps", 65, "db_twist_curl"),

    // TRICEPS (20)
    Workout(81, "Skull Crushers", "Triceps", "Intermediate", "Long head mass.", "Lower EZ-bar to forehead, extend arms.", "3 Sets", "12 Reps", 80, "skull_crushers"),
    Workout(82, "Tricep Pushdowns", "Triceps", "Beginner", "Tricep isolation.", "Push cable bar down to thighs.", "3 Sets", "15 Reps", 60, "tricep_pushdowns"),
    Workout(83, "Close Grip Bench", "Triceps", "Intermediate", "Power compound.", "Press bar with hands shoulder-width.", "4 Sets", "10 Reps", 110, "close_grip_bench"),
    Workout(84, "Overhead DB Ext", "Triceps", "Beginner", "Tricep stretch.", "Press dumbbell above head behind neck.", "3 Sets", "12 Reps", 75, "overhead_db_ext"),
    Workout(85, "Tricep Dips", "Triceps", "Intermediate", "Arm mass builder.", "Keep body upright while dipping.", "3 Sets", "12 Reps", 95, "tricep_dips"),
    Workout(86, "Cable Kickbacks", "Triceps", "Beginner", "Lateral head focus.", "Extend cable back behind torso.", "3 Sets", "15 Reps", 50, "cable_kickbacks"),
    Workout(87, "Bench Dips", "Triceps", "Beginner", "Bodyweight triceps.", "Lower body using a bench behind back.", "3 Sets", "15 Reps", 65, "bench_dips"),
    Workout(88, "Rope Pushdowns", "Triceps", "Beginner", "Contraction focus.", "Push rope down and pull apart.", "3 Sets", "15 Reps", 60, "rope_pushdowns"),
    Workout(89, "Diamond Pushups", "Triceps", "Intermediate", "Tough bodyweight move.", "Pushups with hands in diamond shape.", "3 Sets", "15 Reps", 85, "diamond_pushups"),
    Workout(90, "French Press", "Triceps", "Intermediate", "Long head focus.", "Sitting, lower EZ bar behind head.", "3 Sets", "12 Reps", 80, "french_press"),
    Workout(91, "Single Arm Pushdown", "Triceps", "Beginner", "Isolation focus.", "Push cable down with one hand.", "3 Sets", "15 Reps", 50, "single_arm_pushdown"),
    Workout(92, "Weighted Dips", "Triceps", "Advanced", "Max arm mass.", "Dips with weight belt around waist.", "3 Sets", "8 Reps", 120, "weighted_dips"),
    Workout(93, "Reverse Grip Pushdown", "Triceps", "Intermediate", "Medial head focus.", "Pushdown with palms facing up.", "3 Sets", "12 Reps", 60, "reverse_grip_pushdown"),
    Workout(94, "Tate Press", "Triceps", "Advanced", "Elbow stability/Mass.", "Lying on bench, flare elbows out and press.", "3 Sets", "12 Reps", 85, "tate_press"),
    Workout(95, "JM Press", "Triceps", "Advanced", "Hybrid press/Ext.", "Half press, half extension on bench.", "3 Sets", "10 Reps", 100, "jm_press"),
    Workout(96, "Overhead Cable Ext", "Triceps", "Intermediate", "Constant stretch.", "Pull cable over head with rope.", "3 Sets", "15 Reps", 75, "overhead_cable_ext"),
    Workout(97, "Floor DB Ext", "Triceps", "Intermediate", "Dead stop focus.", "Lying on floor, extend dumbbells up.", "3 Sets", "12 Reps", 70, "floor_db_ext"),
    Workout(98, "Triangle Pushups", "Triceps", "Intermediate", "Core/Tricep focus.", "Variation of diamond pushup.", "3 Sets", "12 Reps", 80, "triangle_pushups"),
    Workout(99, "Bodyweight Wall Ext", "Triceps", "Advanced", "Bodyweight stretch.", "Forearms against wall, push body back.", "3 Sets", "12 Reps", 65, "wall_extensions"),
    Workout(100, "One Arm DB Kickback", "Triceps", "Beginner", "Classic isolation.", "Lean on bench, extend DB back.", "3 Sets", "12 Reps", 50, "one_arm_db_kickback"),

    // LEGS (20)
    Workout(101, "Squats", "Legs", "Intermediate", "The king of legs.", "Lower hips with barbell on shoulders.", "4 Sets", "10 Reps", 180, "squats"),
    Workout(102, "Leg Press", "Legs", "Beginner", "Quad focus.", "Push platform away with feet.", "3 Sets", "12 Reps", 140, "leg_press"),
    Workout(103, "Romanian DL", "Legs", "Intermediate", "Hamstring focus.", "Hinge at hips, lower bar to mid-shin.", "3 Sets", "12 Reps", 130, "romanian_dl"),
    Workout(104, "Leg Extensions", "Legs", "Beginner", "Quad isolation.", "Kick legs out on machine.", "3 Sets", "15 Reps", 70, "leg_extensions"),
    Workout(105, "Leg Curls", "Legs", "Beginner", "Hamstring isolation.", "Curl legs towards glutes on machine.", "3 Sets", "15 Reps", 65, "leg_curls"),
    Workout(106, "Walking Lunges", "Legs", "Intermediate", "Leg strength.", "Step forward and sink low for each step.", "3 Sets", "20 Steps", 110, "walking_lunges"),
    Workout(107, "Bulgarian Squat", "Legs", "Advanced", "Unilateral focus.", "Squat with one foot elevated behind.", "3 Sets", "12 Reps", 120, "bulgarian_squat"),
    Workout(108, "Calf Raises", "Legs", "Beginner", "Lower leg mass.", "Rise up on your toes with weight.", "4 Sets", "20 Reps", 60, "calf_raises"),
    Workout(109, "Hack Squats", "Legs", "Intermediate", "Fixed path mass.", "Squat on angled machine platform.", "3 Sets", "12 Reps", 150, "hack_squats"),
    Workout(110, "Goblet Squats", "Legs", "Beginner", "Perfect form move.", "Squat holding DB at chest height.", "3 Sets", "15 Reps", 100, "goblet_squats"),
    Workout(111, "Front Squats", "Legs", "Advanced", "Quad/Core focus.", "Squat with bar across front shoulders.", "4 Sets", "8 Reps", 170, "front_squats"),
    Workout(112, "Sumo Deadlift", "Legs", "Advanced", "Glute/Inner thigh.", "Wide stance deadlift to standing.", "4 Sets", "8 Reps", 190, "sumo_deadlift"),
    Workout(113, "Glute Bridges", "Legs", "Beginner", "Glute isolation.", "Lift hips from floor while lying back.", "3 Sets", "15 Reps", 70, "glute_bridges"),
    Workout(114, "Box Jumps", "Legs", "Advanced", "Explosive power.", "Jump from floor onto elevated box.", "3 Sets", "10 Reps", 130, "box_jumps"),
    Workout(115, "Step Ups", "Legs", "Beginner", "Functional balance.", "Step onto box with one foot at a time.", "3 Sets", "12 Reps", 90, "step_ups"),
    Workout(116, "Sissy Squats", "Legs", "Advanced", "Deep quad stretch.", "Lean back while holding onto support.", "3 Sets", "12 Reps", 80, "sissy_squats"),
    Workout(117, "Good Mornings (Legs)", "Legs", "Intermediate", "Posterior chain.", "Hinge forward with bar on shoulders.", "3 Sets", "12 Reps", 100, "legs_good_mornings"),
    Workout(118, "Donkey Calf Raises", "Legs", "Intermediate", "Deep calf stretch.", "Calf raises with weight on lower back.", "3 Sets", "20 Reps", 65, "donkey_calf_raises"),
    Workout(119, "Seated Calf Raises", "Legs", "Beginner", "Soleus focus.", "Calf raises while sitting on machine.", "3 Sets", "20 Reps", 55, "seated_calf_raises"),
    Workout(120, "Hamstring Slides", "Legs", "Intermediate", "Bodyweight hams.", "Slide feet out and in while in bridge.", "3 Sets", "12 Reps", 75, "hamstring_slides"),

    // ABS (20)
    Workout(121, "Plank", "Abs", "Beginner", "Core stability.", "Hold body straight on forearms.", "3 Sets", "Hold 60s", 50, "plank", durationSeconds = 60),
    Workout(122, "Leg Raises", "Abs", "Beginner", "Lower ab focus.", "Lie on back, lift legs to 90 degrees.", "3 Sets", "15 Reps", 45, "leg_raises"),
    Workout(123, "Russian Twists", "Abs", "Beginner", "Oblique strength.", "Rotate torso side to side while sitting.", "3 Sets", "20 Reps", 55, "russian_twists"),
    Workout(124, "Bicycle Crunch", "Abs", "Intermediate", "Total ab focus.", "Opposite elbow to opposite knee.", "3 Sets", "20 Reps", 60, "bicycle_crunch"),
    Workout(125, "Ab Wheel", "Abs", "Advanced", "Advanced strength.", "Roll forward and back on a wheel.", "3 Sets", "12 Reps", 80, "ab_wheel"),
    Workout(126, "Hanging Raises", "Abs", "Advanced", "Lower ab focus.", "Pull knees to chest while hanging.", "3 Sets", "15 Reps", 70, "hanging_raises"),
    Workout(127, "Cable Crunches", "Abs", "Intermediate", "Weighted abs.", "Crunch downward with cable rope.", "3 Sets", "15 Reps", 65, "cable_crunches"),
    Workout(128, "Mountain Climbers", "Abs", "Beginner", "Cardio core.", "Quickly alternate knees in plank.", "3 Sets", "30s", 90, "mountain_climbers", durationSeconds = 30),
    Workout(129, "V-Ups", "Abs", "Advanced", "Intensity focus.", "Touch toes and hands in mid-air.", "3 Sets", "15 Reps", 75, "v_ups"),
    Workout(130, "Side Plank", "Abs", "Beginner", "Oblique isolation.", "Hold body up sideways on one arm.", "3 Sets", "30s each", 40, "side_plank", durationSeconds = 30),
    Workout(131, "Dead Bug", "Abs", "Beginner", "Stability control.", "Opposite arm/leg movement while on back.", "3 Sets", "12 Reps", 30, "dead_bug"),
    Workout(132, "Flutter Kicks", "Abs", "Beginner", "Lower ab burn.", "Alternate small leg kicks while on back.", "3 Sets", "45s", 60, "flutter_kicks", durationSeconds = 45),
    Workout(133, "Toe Touches", "Abs", "Beginner", "Upper ab focus.", "Lift hands towards feet while legs up.", "3 Sets", "20 Reps", 45, "toe_touches"),
    Workout(134, "Windshield Wipers", "Abs", "Advanced", "Oblique control.", "Rotate legs side to side while hanging.", "3 Sets", "10 Reps", 85, "windshield_wipers"),
    Workout(135, "Reverse Crunch", "Abs", "Beginner", "Lower ab isolation.", "Curl hips towards chest while lying.", "3 Sets", "15 Reps", 50, "reverse_crunch"),
    Workout(136, "Hollow Body Hold", "Abs", "Intermediate", "Gymnastic core.", "Legs and shoulders hovering off floor.", "3 Sets", "30s", 55, "hollow_body_hold", durationSeconds = 30),
    Workout(137, "Woodchoppers", "Abs", "Intermediate", "Rotational power.", "Pull cable diagonally across body.", "3 Sets", "15 Reps", 70, "woodchoppers"),
    Workout(138, "Boat Pose", "Abs", "Beginner", "Balance/Core.", "Balance on sit bones with legs up.", "3 Sets", "45s", 40, "boat_pose", durationSeconds = 45),
    Workout(139, "Bird Dog (Abs)", "Abs", "Beginner", "Stability/Back.", "Opposite limb extension on all fours.", "3 Sets", "15 Reps", 30, "abs_bird_dog"),
    Workout(140, "Weighted Situps", "Abs", "Intermediate", "Mass focus.", "Situps holding plate at chest.", "3 Sets", "15 Reps", 70, "weighted_situps"),

    // FOREARMS (20)
    Workout(141, "Wrist Curls", "Forearms", "Beginner", "Inner forearm.", "Rest arms on bench, curl wrists up.", "3 Sets", "15 Reps", 40, "wrist_curls"),
    Workout(142, "Reverse Curls", "Forearms", "Beginner", "Outer forearm.", "Curl bar with palms facing down.", "3 Sets", "12 Reps", 50, "reverse_curls"),
    Workout(143, "Farmer's Walk", "Forearms", "Beginner", "Grip strength.", "Walk while holding heavy dumbbells.", "3 Sets", "40m", 100, "farmers_walk"),
    Workout(144, "Wrist Roller", "Forearms", "Intermediate", "Total forearm burn.", "Roll weight up and down with stick.", "3 Sets", "2 Reps", 45, "wrist_roller"),
    Workout(145, "Plate Pinches", "Forearms", "Intermediate", "Finger strength.", "Hold two plates together with fingers.", "3 Sets", "30s", 30, "plate_pinches", durationSeconds = 30),
    Workout(146, "Behind Back Curls", "Forearms", "Beginner", "Wrist flexors.", "Hold bar behind back, curl wrists.", "3 Sets", "15 Reps", 40, "behind_back_curls"),
    Workout(147, "Forearm Hammer Curls", "Forearms", "Beginner", "Brachioradialis.", "Curl dumbbells, palms facing in.", "3 Sets", "12 Reps", 60, "forearm_hammer_curls"),
    Workout(148, "Dead Hangs", "Forearms", "Intermediate", "Grip endurance.", "Hang from pullup bar for time.", "3 Sets", "Max Time", 50, "dead_hangs", durationSeconds = 60),
    Workout(149, "Towel Pullups", "Forearms", "Advanced", "Extreme grip.", "Pullups while gripping towels.", "3 Sets", "8 Reps", 90, "towel_pullups"),
    Workout(150, "Finger Curls", "Forearms", "Beginner", "Hand strength.", "Let bar roll to fingers, then curl.", "3 Sets", "15 Reps", 35, "finger_curls"),
    Workout(151, "Dumbbell Rotations", "Forearms", "Beginner", "Pronation/Supination.", "Rotate wrist with light DB.", "3 Sets", "15 Reps", 30, "db_rotations"),
    Workout(152, "Fat Grip Curls", "Forearms", "Intermediate", "Grip intensity.", "Curl with thickened bar handles.", "3 Sets", "12 Reps", 75, "fat_grip_curls"),
    Workout(153, "Knuckle Pushups", "Forearms", "Intermediate", "Wrist stability.", "Pushups on your knuckles.", "3 Sets", "15 Reps", 80, "knuckle_pushups"),
    Workout(154, "Barbell Holds", "Forearms", "Intermediate", "Static grip.", "Hold heavy barbell for max time.", "3 Sets", "45s", 60, "barbell_holds", durationSeconds = 45),
    Workout(155, "Rice Bucket Digs", "Forearms", "Beginner", "Hand health.", "Move hands through bucket of rice.", "3 Sets", "1m", 40, "rice_bucket_digs", durationSeconds = 60),
    Workout(156, "Single Arm Hang", "Forearms", "Advanced", "Elite grip.", "Hang from bar with one hand.", "3 Sets", "Max Time", 65, "single_arm_hang", durationSeconds = 30),
    Workout(157, "Towel Rows", "Forearms", "Intermediate", "Pull/Grip combo.", "Row cable using a towel as handle.", "3 Sets", "12 Reps", 85, "towel_rows"),
    Workout(158, "Weighted Wrist Ext", "Forearms", "Beginner", "Forearm top.", "Extend wrist up with light DB.", "3 Sets", "15 Reps", 35, "weighted_wrist_ext"),
    Workout(159, "Block Pinches", "Forearms", "Advanced", "Wide grip focus.", "Pinch thick block and lift.", "3 Sets", "10 Reps", 50, "block_pinches"),
    Workout(160, "Grippers", "Forearms", "Beginner", "Crush strength.", "Squeeze hand grippers shut.", "3 Sets", "20 Reps", 30, "grippers")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(isDarkMode: Boolean, onBack: () -> Unit) {
    var selectedMuscle by remember { mutableStateOf("All") }
    var selectedWorkout by remember { mutableStateOf<Workout?>(null) }
    
    val bgColor = if (isDarkMode) DeepSlate else Color(0xFFF8FAFC)
    val mainTextColor = if (isDarkMode) PureWhite else DeepSlate
    val secondaryTextColor = mainTextColor.copy(alpha = 0.6f)

    val muscles = listOf("All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Abs", "Forearms")
    val filteredWorkouts = if (selectedMuscle == "All") workoutData else workoutData.filter { it.muscleGroup == selectedMuscle }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TRAINING HUB", fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp, color = mainTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.background(mainTextColor.copy(0.05f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 20.dp).fillMaxSize()) {
            
            Text("SELECT TARGET", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(muscles) { muscle ->
                    FilterChip(
                        selected = selectedMuscle == muscle,
                        onClick = { selectedMuscle = muscle },
                        label = { Text(muscle, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if(isDarkMode) MutedSlate else PureWhite,
                            labelColor = secondaryTextColor,
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = PureWhite
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedMuscle == muscle,
                            borderColor = secondaryTextColor.copy(0.1f),
                            selectedBorderColor = EmeraldPrimary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredWorkouts) { workout ->
                    WorkoutPremiumCard(workout, isDarkMode, mainTextColor) { selectedWorkout = workout }
                }
            }
        }
    }

    if (selectedWorkout != null) {
        WorkoutDetailModal(workout = selectedWorkout!!, isDarkMode, mainTextColor) { selectedWorkout = null }
    }
}

@Composable
fun WorkoutPremiumCard(workout: Workout, isDarkMode: Boolean, textColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkMode) MutedSlate else PureWhite,
        border = BorderStroke(1.dp, textColor.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).background(EmeraldPrimary.copy(0.1f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = EmeraldPrimary, modifier = Modifier.size(30.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(workout.muscleGroup, color = textColor.copy(0.5f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                DifficultyTag(workout.difficulty)
                Text("~${workout.caloriesBurned} kcal", color = EmeraldPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DifficultyTag(difficulty: String) {
    val color = when(difficulty) {
        "Beginner" -> EmeraldPrimary
        "Intermediate" -> SkyAccent
        "Advanced" -> RoseAccent
        else -> Color.Gray
    }
    Text(
        text = difficulty.uppercase(),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun WorkoutDetailModal(workout: Workout, isDarkMode: Boolean, textColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkMode) MutedSlate else PureWhite,
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary), shape = RoundedCornerShape(12.dp)) {
                Text("DONE", fontWeight = FontWeight.Black)
            }
        },
        title = {
            Column {
                Text(workout.name.uppercase(), color = textColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                DifficultyTag(workout.difficulty)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(20.dp), color = textColor.copy(0.05f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayCircle, null, tint = EmeraldPrimary, modifier = Modifier.size(60.dp))
                    }
                }
                
                DetailRow("TARGET", workout.muscleGroup, Icons.Rounded.TrackChanges, textColor)
                DetailRow("ROUTINE", "${workout.sets} x ${workout.reps}", Icons.Rounded.Loop, textColor)
                DetailRow("PERFORM", workout.position, Icons.Rounded.Info, textColor)
                
                if (workout.durationSeconds != null) {
                    PremiumTimer(workout.durationSeconds, textColor)
                }
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = textColor.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PremiumTimer(seconds: Int, textColor: Color) {
    var timeLeft by remember { mutableIntStateOf(seconds) }
    var isRunning by remember { mutableStateOf(false) }
    
    LaunchedEffect(isRunning) {
        if (isRunning && timeLeft > 0) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            isRunning = false
        }
    }
    
    Surface(
        color = EmeraldPrimary.copy(0.05f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${timeLeft}s", color = textColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
            IconButton(onClick = { isRunning = !isRunning }, modifier = Modifier.background(if(isRunning) RoseAccent.copy(0.1f) else EmeraldPrimary.copy(0.1f), CircleShape)) {
                Icon(if(isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if(isRunning) RoseAccent else EmeraldPrimary)
            }
        }
    }
}
