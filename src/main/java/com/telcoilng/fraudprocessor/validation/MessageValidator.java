package com.telcoilng.fraudprocessor.validation;

import com.telcoilng.fraudprocessor.Entity.Terminals;
import org.jpos.iso.ISOMsg;

import java.util.function.Function;

public interface MessageValidator{
    Function<ISOMsg, Terminals> _validateTerminalMerchantClient();

    Function<ISOMsg,Boolean> _0800Validator();

    Function<ISOMsg,Boolean> _0200Validator();

    Function<ISOMsg,Boolean> _0100Validator();

    Function<ISOMsg,Boolean> _0420Validator();

    Function<ISOMsg,Boolean> _1100Validator();

}
