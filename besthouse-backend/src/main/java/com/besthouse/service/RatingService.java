package com.besthouse.service;

import com.besthouse.dto.RatingBatchDto;
import com.besthouse.dto.RatingEntryDto;
import com.besthouse.dto.RatingViewDto;
import com.besthouse.entity.House;
import com.besthouse.entity.HouseRating;
import com.besthouse.entity.Member;
import com.besthouse.entity.RatingDimension;
import com.besthouse.exception.ResourceNotFoundException;
import com.besthouse.repository.HouseRatingRepository;
import com.besthouse.repository.HouseRepository;
import com.besthouse.repository.MemberRepository;
import com.besthouse.repository.RatingDimensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final HouseRatingRepository houseRatingRepository;
    private final HouseRepository houseRepository;
    private final MemberRepository memberRepository;
    private final RatingDimensionRepository dimensionRepository;

    @Transactional(readOnly = true)
    public List<RatingViewDto> getRatingsByHouse(Long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw new ResourceNotFoundException("找不到房屋 ID: " + houseId);
        }
        return houseRatingRepository.findByHouseIdWithDetails(houseId)
                .stream()
                .map(this::toViewDto)
                .toList();
    }

    @Transactional
    public List<RatingViewDto> saveRatings(Long houseId, RatingBatchDto batchDto) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到房屋 ID: " + houseId));

        for (RatingEntryDto entry : batchDto.getRatings()) {
            Member member = memberRepository.findById(entry.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("找不到成員 ID: " + entry.getMemberId()));
            RatingDimension dimension = dimensionRepository.findById(entry.getDimensionId())
                    .orElseThrow(() -> new ResourceNotFoundException("找不到評分維度 ID: " + entry.getDimensionId()));

            Optional<HouseRating> existing = houseRatingRepository
                    .findByHouseHouseIdAndMemberMemberIdAndDimensionDimensionId(
                            houseId, entry.getMemberId(), entry.getDimensionId());

            if (existing.isPresent()) {
                existing.get().setScore(entry.getScore());
                houseRatingRepository.save(existing.get());
            } else {
                HouseRating newRating = HouseRating.builder()
                        .house(house)
                        .member(member)
                        .dimension(dimension)
                        .score(entry.getScore())
                        .build();
                houseRatingRepository.save(newRating);
            }
        }

        log.info("儲存評分: houseId={}, count={}", houseId, batchDto.getRatings().size());
        return getRatingsByHouse(houseId);
    }

    private RatingViewDto toViewDto(HouseRating rating) {
        return RatingViewDto.builder()
                .ratingId(rating.getRatingId())
                .memberId(rating.getMember().getMemberId())
                .memberName(rating.getMember().getDisplayName())
                .dimensionId(rating.getDimension().getDimensionId())
                .dimensionName(rating.getDimension().getDimensionName())
                .score(rating.getScore())
                .build();
    }
}
