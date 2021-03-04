/*
 * Copyright 2021 Paul Rybitskyi, paul.rybitskyi.work@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.paulrybitskyi.gamedge.data.games

import com.github.michaelbull.result.get
import com.paulrybitskyi.gamedge.data.commons.ErrorMapper
import com.paulrybitskyi.gamedge.data.games.datastores.GamesDataStores
import com.paulrybitskyi.gamedge.data.games.usecases.RefreshCompanyDevelopedGamesUseCaseImpl
import com.paulrybitskyi.gamedge.data.games.usecases.commons.GameMapper
import com.paulrybitskyi.gamedge.data.games.usecases.commons.RefreshGamesUseCaseMappers
import com.paulrybitskyi.gamedge.data.games.usecases.commons.mapToDomainGames
import com.paulrybitskyi.gamedge.data.games.usecases.commons.throttling.GamesRefreshingThrottlerTools
import com.paulrybitskyi.gamedge.data.games.utils.DATA_GAMES
import com.paulrybitskyi.gamedge.data.games.utils.FakeDispatcherProvider
import com.paulrybitskyi.gamedge.data.games.utils.FakeGamesLocalDataStore
import com.paulrybitskyi.gamedge.data.games.utils.FakeGamesRefreshingThrottler
import com.paulrybitskyi.gamedge.data.games.utils.FakeGamesRefreshingThrottlerKeyProvider
import com.paulrybitskyi.gamedge.data.games.utils.FakeGamesRemoteDataStore
import com.paulrybitskyi.gamedge.domain.commons.DomainPagination
import com.paulrybitskyi.gamedge.domain.games.DomainCompany
import com.paulrybitskyi.gamedge.domain.games.usecases.RefreshCompanyDevelopedGamesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test


private val DOMAIN_COMPANY = DomainCompany(
    id = 1,
    name = "company",
    websiteUrl = "url",
    logo = null,
    developedGames = listOf(1, 2, 3)
)

private val USE_CASE_PARAMS = RefreshCompanyDevelopedGamesUseCase.Params(DOMAIN_COMPANY, DomainPagination())


internal class RefreshCompanyDevelopedGamesUseCaseImplTest {


    private lateinit var gamesLocalDataStore: FakeGamesLocalDataStore
    private lateinit var gamesRemoteDataStore: FakeGamesRemoteDataStore
    private lateinit var throttler: FakeGamesRefreshingThrottler
    private lateinit var gameMapper: GameMapper
    private lateinit var SUT: RefreshCompanyDevelopedGamesUseCaseImpl


    @Before
    fun setup() {
        gamesLocalDataStore = FakeGamesLocalDataStore()
        gamesRemoteDataStore = FakeGamesRemoteDataStore()
        throttler = FakeGamesRefreshingThrottler()
        gameMapper = GameMapper()
        SUT = RefreshCompanyDevelopedGamesUseCaseImpl(
            gamesDataStores = GamesDataStores(
                local = gamesLocalDataStore,
                remote = gamesRemoteDataStore
            ),
            dispatcherProvider = FakeDispatcherProvider(),
            throttlerTools = GamesRefreshingThrottlerTools(
                throttler = throttler,
                keyProvider = FakeGamesRefreshingThrottlerKeyProvider()
            ),
            mappers = RefreshGamesUseCaseMappers(
                game = gameMapper,
                error = ErrorMapper()
            )
        )
    }


    @Test
    fun `Emits remote games when refresh is possible`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = true
            gamesRemoteDataStore.shouldReturnGames = true

            assertThat(SUT.execute(USE_CASE_PARAMS).first().get())
                .isEqualTo(gameMapper.mapToDomainGames(DATA_GAMES))
        }
    }


    @Test
    fun `Does not emit remote games when refresh is not possible`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = false

            var isEmptyFlow = false

            SUT.execute(USE_CASE_PARAMS)
                .onEmpty { isEmptyFlow = true }
                .firstOrNull()

            assertThat(isEmptyFlow).isTrue
        }
    }


    @Test
    fun `Saves remote games into local data store when refresh is successful`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = true
            gamesRemoteDataStore.shouldReturnGames = true

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(gamesLocalDataStore.games)
                .isEqualTo(DATA_GAMES)
        }
    }


    @Test
    fun `Does not save remote games into local data store when refresh is not possible`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = false

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(gamesLocalDataStore.games.isEmpty()).isTrue
        }
    }


    @Test
    fun `Does not save remote games into local data store when refresh is unsuccessful`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = false
            gamesRemoteDataStore.shouldReturnError = true

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(gamesLocalDataStore.games.isEmpty()).isTrue
        }
    }


    @Test
    fun `Updates games last refresh time when refresh is successful`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = true
            gamesRemoteDataStore.shouldReturnGames = true

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(throttler.areGamesLastRefreshTimeUpdated).isTrue
        }
    }


    @Test
    fun `Does not update games last refresh time when refresh is not possible`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = false

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(throttler.areGamesLastRefreshTimeUpdated).isFalse
        }
    }


    @Test
    fun `Does not update games last refresh time when refresh is unsuccessful`() {
        runBlockingTest {
            throttler.canRefreshCompanyDevelopedGames = false
            gamesRemoteDataStore.shouldReturnError = true

            SUT.execute(USE_CASE_PARAMS).firstOrNull()

            assertThat(throttler.areGamesLastRefreshTimeUpdated).isFalse
        }
    }


}