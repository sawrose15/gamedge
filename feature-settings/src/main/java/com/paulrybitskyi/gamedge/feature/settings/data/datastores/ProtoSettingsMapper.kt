/*
 * Copyright 2022 Paul Rybitskyi, paul.rybitskyi.work@gmail.com
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

package com.paulrybitskyi.gamedge.feature.settings.data.datastores

import com.paulrybitskyi.gamedge.feature.settings.data.DataSettings
import com.paulrybitskyi.gamedge.feature.settings.data.DataTheme
import javax.inject.Inject

internal class ProtoSettingsMapper @Inject constructor() {

    fun mapToProtoSettings(settings: DataSettings): ProtoSettings {
        return ProtoSettings.newBuilder()
            .setThemeName(settings.theme.name)
            .build()
    }

    fun mapToDataSettings(protoSettings: ProtoSettings): DataSettings {
        return DataSettings(
            theme = DataTheme.valueOf(protoSettings.themeName),
        )
    }
}