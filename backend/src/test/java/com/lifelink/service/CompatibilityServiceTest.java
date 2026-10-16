package com.lifelink.service;

import com.lifelink.entity.BloodGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatibilityServiceTest {
    private final CompatibilityService svc = new CompatibilityService();

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "O_NEGATIVE,AB_POSITIVE,true",
            "O_NEGATIVE,O_NEGATIVE,true",
            "O_POSITIVE,O_NEGATIVE,false",
            "A_POSITIVE,O_POSITIVE,false",
            "A_POSITIVE,AB_POSITIVE,true",
            "A_NEGATIVE,A_POSITIVE,true",
            "A_POSITIVE,A_NEGATIVE,false",
            "B_POSITIVE,A_POSITIVE,false",
            "B_NEGATIVE,AB_NEGATIVE,true",
            "AB_POSITIVE,A_POSITIVE,false",
            "AB_NEGATIVE,AB_POSITIVE,true",
            "AB_POSITIVE,AB_POSITIVE,true"})
    void matrix(BloodGroup donor, BloodGroup recipient, boolean expected) {
        assertEquals(expected, svc.canDonate(donor, recipient));
    }

    @Test
    void onlyONegativeDonorsCanGiveToONegative() {
        assertEquals(List.of(BloodGroup.O_NEGATIVE), svc.compatibleDonorGroups(BloodGroup.O_NEGATIVE));
    }

    @Test
    void abPositiveRecipientAcceptsAllEightGroups() {
        assertEquals(8, svc.compatibleDonorGroups(BloodGroup.AB_POSITIVE).size());
    }
}