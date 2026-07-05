package com.example.domain.repository

import com.example.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    val allProjects: Flow<List<ProjectEntity>>
    suspend fun getProjectById(id: Int): ProjectEntity?
    suspend fun insertProject(project: ProjectEntity): Long
    suspend fun updateProject(project: ProjectEntity)
    suspend fun deleteProject(project: ProjectEntity)
    suspend fun deleteProjectById(id: Int)
}
