package de.xikolo.viewmodels.course

import androidx.lifecycle.LiveData
import de.xikolo.models.Course
import de.xikolo.models.dao.CourseDao
import de.xikolo.network.jobs.GetCourseJob
import de.xikolo.network.jobs.base.NetworkStateLiveData
import de.xikolo.viewmodels.base.BaseViewModel

class CourseViewModel(val courseId: String) : BaseViewModel() {

    private val coursesDao = CourseDao(realm)

    val course: LiveData<Course> by lazy {
        coursesDao.find(courseId)
    }

    override fun onFirstCreate() {
        requestCourse(false)
    }

    override fun onRefresh() {
        requestCourse(true)
    }

    private fun requestCourse(userRequest: Boolean) {
        GetCourseJob(courseId, networkState, userRequest).run()
    }

    fun requestCourse(userRequest: Boolean, networkState: NetworkStateLiveData) {
        GetCourseJob(courseId, networkState, userRequest).run()
    }
}
