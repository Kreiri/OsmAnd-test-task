package com.example.osmandtesttask.data.remote.dto

import com.example.osmandtesttask.domain.models.RegionsList
import com.example.osmandtesttask.domain.models.Region

class RemoteRegionsList(
    val regions: List<RemoteRegion>
) {
    fun toLocal(): RegionsList {
        val regions = regionsToLocal()
        return RegionsList(regions)
    }

    private class RemoteNode(val region: RemoteRegion, val affixes: DownloadAffixes)

    private fun regionsToLocal(): List<Region> {
        val remotesStack = ArrayDeque<RemoteNode>()
        val iteratorStack = ArrayDeque<Iterator<RemoteRegion>>()
        val locals = mutableListOf<Region>()

        iteratorStack.addLast(regions.iterator())
        while (iteratorStack.isNotEmpty()) {
            val currentIterator = iteratorStack.last()
            if (currentIterator.hasNext()) {
                val currentRemote = currentIterator.next()
                val parentData = remotesStack.lastOrNull()
                val affixes = currentRemote.mergeDownloadAffixes(parentData?.affixes)
                remotesStack.addLast(RemoteNode(currentRemote, affixes))
                iteratorStack.addLast(currentRemote.regions.iterator())
            } else {
                iteratorStack.removeLast()
                if (remotesStack.isEmpty()) continue
                val finishedRemote = remotesStack.removeLast()
                val childCount = finishedRemote.region.regions.size

                val compiledChildren = locals.takeLast(childCount)
                locals.subList(locals.size - childCount, locals.size).clear()

                val parentRemote = remotesStack.lastOrNull()
                val finalDomainNode = finishedRemote.region.toLocal(parentRemote?.affixes, compiledChildren)
                locals.add(finalDomainNode)
            }
        }
        return locals.toList()
    }
}
