package uz.oybek.ozamiz.data.lessons

import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.AudioUploader
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.firebasePrefix
import uz.oybek.ozamiz.ui.showToast

object LessonsController {

    private val lessonsCollection = FirebaseInstances.firestore.collection("lessons")
    private val courses = FirebaseInstances.firestore.collection("courses")
    private val tests = FirebaseInstances.firestore.collection("tests")
    private val storage = FirebaseInstances.storage.getReference("recordings")

    fun loadLessons(
        startOrderId: String, limit: Int = 12, loaded: (lessons: List<LessonModel>?) -> Unit
    ) {
        var query = lessonsCollection.limit(limit.toLong()).orderBy(LessonModel::order.name)
        if (startOrderId.isNotEmpty()) {
            query = query.startAfter(startOrderId)
        }
        query.get().addOnSuccessListener {
            val lessons = it.toObjects(LessonModel::class.java)
            loaded(lessons)
        }.addOnFailureListener {
            loaded(null)
        }
    }

    fun getCourses(lessonId: String, loaded: (courses: List<CourseModel>?) -> Unit) {
        courses.whereEqualTo(CourseModel::lessonId.name, lessonId)
            .orderBy(CourseModel::order.name).get().addOnSuccessListener {
            val coursesList = it.toObjects(CourseModel::class.java)
            loaded(coursesList)
        }.addOnFailureListener {
            loaded(null)
        }
    }

    fun getTests(lessonId: String, loaded: (tests: List<CourseTestModel>?) -> Unit) {
        tests.whereEqualTo(CourseTestModel::lessonId.name, lessonId)
            .orderBy(CourseTestModel::order.name)
            .get()
            .addOnSuccessListener {
            val testsList = it.toObjects(CourseTestModel::class.java)
            loaded(testsList)
        }.addOnFailureListener {
            loaded(null)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun addOrUpdateLesson(lesson: LessonModel, added: (success: Boolean) -> Unit) {
        val updateLesson = Runnable {
            lessonsCollection.document(lesson.id).set(lesson).addOnSuccessListener {
                added(true)
            }.addOnFailureListener {
                added(false)
            }
        }
        if (lesson.imageUrl.startsWith(firebasePrefix)) {
            updateLesson.run()
        } else {
            GlobalScope.launch {
                val imageUploadResult =
                    ImageUploader.uploadImage(lesson.imageUrl.toUri(), ImageUploader.OZAMIZ_PATH)
                if (imageUploadResult is DataWrapper.Success) {
                    lesson.imageUrl = imageUploadResult.data
                    updateLesson.run()
                } else {
                    added(false)
                }
            }
        }
    }

    fun deleteLesson(lessonId: String, deleted: (success: Boolean) -> Unit) {
        lessonsCollection.document(lessonId).delete().addOnSuccessListener {
            deleted(true)
        }.addOnFailureListener {
            deleted(false)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun addEditCourse(course: CourseModel, added: (success: Boolean) -> Unit) {
        val updateCourse = Runnable {
            val update = Runnable {
                courses.document(course.id).set(course).addOnSuccessListener {
                    added(true)
                }.addOnFailureListener {
                    added(false)
                }
            }
            if (course.voiceUrl.isNotEmpty()) {
                if (course.voiceUrl.startsWith(firebasePrefix)) {
                    update.run()
                } else {
                    GlobalScope.launch {
                        val audioUploadResult = AudioUploader.uploadAudio(course.voiceUrl.toUri())
                        if (audioUploadResult is DataWrapper.Success) {
                            course.voiceUrl = audioUploadResult.data
                            update.run()
                        } else {
                            added(false)
                        }
                    }
                }
            } else {
                update.run()
            }
        }
        if (course.imageUrl.startsWith(firebasePrefix)) {
            updateCourse.run()
        } else {
            GlobalScope.launch {
                val imageUploadResult =
                    ImageUploader.uploadImage(course.imageUrl.toUri(), ImageUploader.OZAMIZ_PATH)
                if (imageUploadResult is DataWrapper.Success) {
                    course.imageUrl = imageUploadResult.data
                    updateCourse.run()
                } else {
                    added(false)
                }
            }
        }
    }
}