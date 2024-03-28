package org.mslowko.turnbasedfighter.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.mslowko.turnbasedfighter.model.Character;
import org.mslowko.turnbasedfighter.model.Dungeon;
import org.mslowko.turnbasedfighter.model.repository.DungeonRepository;
import org.mslowko.turnbasedfighter.pojo.dto.DungeonDto;
import org.mslowko.turnbasedfighter.pojo.exceptions.*;
import org.mslowko.turnbasedfighter.pojo.requests.CharacterJoinRequest;
import org.mslowko.turnbasedfighter.pojo.responses.DungeonCreateResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DungeonService {
    private final DungeonRepository dungeonRepository;
    private final CharacterService characterService;
    private final ModelMapper modelMapper;

    public DungeonCreateResponse createDungeon(int waves, int slots) {
        Dungeon dungeon = new Dungeon(waves, slots);
        dungeonRepository.save(dungeon);
        DungeonDto dungeonDto = modelMapper.map(dungeon, DungeonDto.class);
        return new DungeonCreateResponse(dungeonDto, dungeon.getJoinKey());
    }

    public DungeonDto startDungeon(String id) {
        Dungeon dungeon = fetchDungeon(id);
        if (dungeon.isStarted())
            throw new DungeonAlreadyStartedException(dungeon.getId());

        dungeon.setStarted(true);
        dungeonRepository.save(dungeon);
        return modelMapper.map(dungeon, DungeonDto.class);
    }

    public DungeonDto joinDungeon(String id, CharacterJoinRequest joinDto) {
        Dungeon dungeon = fetchDungeon(id);
        Character character = characterService.fetchCharacter(joinDto.getCharacterId());
        validateJoinPrerequisites(dungeon, character, joinDto.getKey());
        dungeon.getLobby().add(character);
        dungeonRepository.save(dungeon);
        return modelMapper.map(dungeon, DungeonDto.class);
    }

    private void validateJoinPrerequisites(Dungeon dungeon, Character character, String key) {
        if (dungeon.isStarted())
            throw new DungeonAlreadyStartedException(dungeon.getId());
        if (!Objects.equals(dungeon.getJoinKey(), key))
            throw new InvalidJoinKeyException();
        if (dungeon.getLobby().size() == dungeon.getSlots())
            throw new DungeonAlreadyFullException(dungeon.getId(), dungeon.getSlots());
        if (dungeonRepository.fetchDungeonByCharacterName(character.getName()).isPresent())
            throw new CharacterAlreadyDeployedException(character.getName(), dungeon.getId());
    }

    private Dungeon fetchDungeon(String id) {
        Optional<Dungeon> optionalDungeon = dungeonRepository.findById(id);
        if (optionalDungeon.isEmpty())
            throw new DungeonNotFoundException(id);
        return optionalDungeon.get();
    }
}
