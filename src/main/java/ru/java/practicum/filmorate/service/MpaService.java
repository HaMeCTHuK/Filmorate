package ru.java.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.java.practicum.filmorate.model.Mpa;
import ru.java.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Service
@Slf4j
public class MpaService extends AbstractService<Mpa> {

    private final MpaStorage mpaStorage; //добавлено для работоспособности тестов
    public MpaService(MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage; //добавлено для работоспособности тестов
        this.abstractStorage = mpaStorage;
    }

    public List<Mpa> getAllMpa() {
        return abstractStorage.getAll();
    }

    public Mpa getMpa(long id) {
        return abstractStorage.get(id);
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

    public Mpa createMpa(int id, String name) {
        return mpaStorage.create(Mpa.builder().id(id).name(name).build());
        //Mpa mpa = Mpa.builder().id(id).name(name).build();
        //return create(mpa);
    }
}
