package com.careercoach.common.util

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object PageUtils {
    
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    
    fun createPageable(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        sortBy: String? = null,
        sortDirection: Sort.Direction = Sort.Direction.DESC
    ): Pageable {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        
        return if (sortBy != null) {
            PageRequest.of(page, pageSize, Sort.by(sortDirection, sortBy))
        } else {
            PageRequest.of(page, pageSize)
        }
    }
    
    fun createPageable(
        page: Int = 0,
        size: Int = DEFAULT_PAGE_SIZE,
        sort: Sort
    ): Pageable {
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        return PageRequest.of(page, pageSize, sort)
    }
}