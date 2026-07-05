package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class ProjectRepositoryImpl(private val projectDao: ProjectDao) : ProjectRepository {
    override val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    override suspend fun getProjectById(id: Int): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    override suspend fun insertProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    override suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    override suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    override suspend fun deleteProjectById(id: Int) {
        projectDao.deleteProjectById(id)
    }
}
