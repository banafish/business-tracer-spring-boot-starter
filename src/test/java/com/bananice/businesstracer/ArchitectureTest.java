package com.bananice.businesstracer;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * DDD 分层守护。规则与 docs/superpowers/specs/2026-07-10-engineering-harness-design.md 一致。
 * 存量违规由 FreezingArchRule 冻结在 src/test/resources/archunit-store，只拦截新增违规；
 * 消除一条存量违规后重跑测试，store 会自动收缩，把收缩后的 store 一并提交。
 */
@AnalyzeClasses(packages = "com.bananice.businesstracer", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothing = noClasses()
            .that()
            .resideInAPackage("..businesstracer.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..businesstracer.api..",
                    "..businesstracer.application..",
                    "..businesstracer.config..",
                    "..businesstracer.infrastructure..",
                    "..businesstracer.presentation..");

    @ArchTest
    static final ArchRule applicationOnlyDependsOnDomain = FreezingArchRule.freeze(noClasses()
            .that()
            .resideInAPackage("..businesstracer.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..businesstracer.api..",
                    "..businesstracer.infrastructure..",
                    "..businesstracer.presentation..",
                    "..businesstracer.config.."));

    @ArchTest
    static final ArchRule apiDoesNotDependOnInfrastructure = FreezingArchRule.freeze(noClasses()
            .that()
            .resideInAPackage("..businesstracer.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..businesstracer.infrastructure.."));

    @ArchTest
    static final ArchRule noPackageCycles = FreezingArchRule.freeze(SlicesRuleDefinition.slices()
            .matching("com.bananice.businesstracer.(*)..")
            .should()
            .beFreeOfCycles());
}
