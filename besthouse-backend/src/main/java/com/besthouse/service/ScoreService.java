package com.besthouse.service;

import com.besthouse.dto.ScoreResultDto;
import com.besthouse.entity.House;
import com.besthouse.entity.HouseRating;
import com.besthouse.entity.Member;
import com.besthouse.entity.RatingDimension;
import com.besthouse.entity.enums.HouseStatus;
import com.besthouse.repository.HouseRatingRepository;
import com.besthouse.repository.HouseRepository;
import com.besthouse.repository.MemberRepository;
import com.besthouse.repository.RatingDimensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final HouseRepository houseRepository;
    private final MemberRepository memberRepository;
    private final RatingDimensionRepository dimensionRepository;
    private final HouseRatingRepository houseRatingRepository;

    /**
     * 計算所有 ACTIVE 房屋的加權總分並排名。
     * 公式：totalScore = Σ[ memberWeight × Σ(dimensionScore × dimensionWeight) ]
     */
    @Transactional(readOnly = true)
    public List<ScoreResultDto> calculateRanking() {
        List<House> activeHouses = houseRepository.findByStatusOrderByCreatedAtDesc(HouseStatus.ACTIVE);
        List<Member> members = memberRepository.findAllByOrderBySortOrderAsc();
        List<RatingDimension> dimensions = dimensionRepository.findByIsActiveTrueOrderByWeightDescSortOrderAsc();
        List<HouseRating> allRatings = houseRatingRepository.findAllWithDetails();

        // key: houseId-memberId-dimensionId -> score
        Map<String, Integer> ratingMap = allRatings.stream()
                .collect(Collectors.toMap(
                        r -> ratingKey(r.getHouse().getHouseId(),
                                r.getMember().getMemberId(),
                                r.getDimension().getDimensionId()),
                        HouseRating::getScore
                ));

        List<ScoreResultDto> results = new ArrayList<>();

        for (House house : activeHouses) {
            double totalScore = 0.0;
            Map<String, Double> memberScores = new LinkedHashMap<>();

            for (Member member : members) {
                double memberScore = 0.0;
                for (RatingDimension dimension : dimensions) {
                    String key = ratingKey(house.getHouseId(), member.getMemberId(), dimension.getDimensionId());
                    Integer score = ratingMap.get(key);
                    if (score != null) {
                        memberScore += score * dimension.getWeight().doubleValue();
                    }
                }
                totalScore += member.getWeight().doubleValue() * memberScore;
                memberScores.put(member.getDisplayName(),
                        Math.round(memberScore * 100.0) / 100.0);
            }

            results.add(ScoreResultDto.builder()
                    .houseId(house.getHouseId())
                    .nickname(house.getNickname())
                    .address(house.getAddress())
                    .totalScore(Math.round(totalScore * 100.0) / 100.0)
                    .memberScores(memberScores)
                    .build());
        }

        // 依總分降冪排序後補上名次
        results.sort(Comparator.comparingDouble(ScoreResultDto::getTotalScore).reversed());
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results;
    }

    private String ratingKey(Long houseId, Long memberId, Long dimensionId) {
        return houseId + "-" + memberId + "-" + dimensionId;
    }
}
