package com.nvsp.manta_terminal.evidence

import com.nvsp.nvmesapplibrary.architecture.CommunicationViewModel
import com.nvsp.nvmesapplibrary.communication.volley.ServiceVolley
import com.nvsp.nvmesapplibrary.database.LibRepository

class EvidenceViewModel(repository: LibRepository, private val api: ServiceVolley) : CommunicationViewModel(repository, api){
}