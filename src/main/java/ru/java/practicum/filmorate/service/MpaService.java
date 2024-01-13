package ru.java.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.java.practicum.filmorate.model.Mpa;
import ru.java.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Service
@Slf4j
public class MpaService extends AbstractService<Mpa> {

    private final MpaStorage mpaStorage;

    public MpaService(MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    public List<Mpa> getAllMpa() {
        return mpaStorage.getAll();
    }

    public Mpa getMpa(long id) {
        return mpaStorage.get(id);
    }

    @Override
    public void validate(Mpa data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateParameter(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateParameters(Long id, Long otherId) {
        throw new UnsupportedOperationException();
    }
}
