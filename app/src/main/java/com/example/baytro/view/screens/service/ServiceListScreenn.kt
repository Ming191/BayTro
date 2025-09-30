

@Composable
fun ServiceListScreen(

) {
    ServiceListContent(

    )
}

@Composable
fun ServiceListContent(

) {
  Scaffold(
    content = { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize(innerPadding)
          .padding(16.dp)
      ) {
        item{
          DividerWithSubhead(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
              subhead = "ServiceList"
          )
          DropdownSelectField(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            label = "Select building",
            /*
            options = formState.availableBuildings,
            selectedOption = formState.selectedBuilding,
            onOptionSelected = onBuildingSelected,
            optionToString = { it.name },
            enabled = formState.availableBuildings.isNotEmpty()
            */
          )
        }
        
        item{

        }
      }
    }
  )
}

@Preview(showBackground = true)
@Composable
fun ServiceListPreview() {
    AppTheme {
        ServiceListContent(
            services = listOf(
                Service(
                    id = "1",
                    buildingId = "B01",
                    roomId = listOf("R101","R102").joinToString(","), 
                    pricing_type = "Internet",
                    price_per_unit = "100.000 VND/room",
                    created_at = "",
                    updated_at = "",
                    status = com.example.baytro.data.service.Status.ACTIVE
                ),
                Service(
                    id = "2",
                    buildingId = "B01",
                    roomId = listOf("R201").joinToString(","),
                    pricing_type = "Electricity",
                    price_per_unit = "4.000 VND/kWh",
                    created_at = "",
                    updated_at = "",
                    status = com.example.baytro.data.service.Status.ACTIVE
                )
            ),
            onEditService = {},
            onDeleteService = {},
            onAddService = {}
        )
    }
}
