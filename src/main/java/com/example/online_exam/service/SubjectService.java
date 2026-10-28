package com.example.online_exam.service;

import com.example.online_exam.entity.Subject;
import com.example.online_exam.exception.DuplicateResourceException;
import com.example.online_exam.exception.ResourceNotFoundException;
import com.example.online_exam.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public Subject createSubject(String name, String description) {
        if (subjectRepository.existsByName(name)) {
            throw new DuplicateResourceException("Subject with name '" + name + "' already exists");
        }
        return subjectRepository.save(new Subject(name, description));
    }

    public Subject updateSubject(Long id, String name, String description) {
        Subject subject = findById(id);
        // Allow rename only if new name doesn't conflict with another record
        if (!subject.getName().equals(name) && subjectRepository.existsByName(name)) {
            throw new DuplicateResourceException("Subject with name '" + name + "' already exists");
        }
        subject.setName(name);
        subject.setDescription(description);
        return subjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public Subject findById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    @Transactional(readOnly = true)
    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    public void deleteSubject(Long id) {
        Subject subject = findById(id);
        subjectRepository.delete(subject);
    }
}
