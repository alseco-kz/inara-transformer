function SaveClientLastData(){
    $.client.LastCallDate = moment(currentDate());
    if (FindAccountIsAccountSet()){
        $.client.AccountId = GetAccountNumber()
        $.client.DateAccount = moment(currentDate())
    }
}