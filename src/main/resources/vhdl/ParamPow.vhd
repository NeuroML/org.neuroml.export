

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use IEEE.numeric_std.all;

entity ParamPow is
	generic( 
		BIT_TOP 	: integer := 20;	
		BIT_BOTTOM	: integer := -20);	
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		A		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		X		: In sfixed(BIT_TOP downto BIT_BOTTOM);
		Output	: Out sfixed(BIT_TOP downto BIT_BOTTOM)
		);
end ParamPow; 

architecture RTL of ParamPow is
signal output_internal : sfixed(BIT_TOP downto BIT_BOTTOM);
signal count : sfixed(BIT_TOP downto BIT_BOTTOM);
begin
	 
	process(clk,rst)
		variable Sel : integer;
		begin
			if rst = '1' then
				count <= to_sfixed(1,BIT_TOP, BIT_BOTTOM);
				output_internal <= to_sfixed (0,BIT_TOP, BIT_BOTTOM);
				Done <= '0';
			elsif clk'event and clk = '1' then
				if Start = '1' then
					count <= to_sfixed(1,BIT_TOP, BIT_BOTTOM);
					output_internal <= A;
					Done <= '0';
				else
					if To_slv ( resize (count - X   ,BIT_TOP, BIT_BOTTOM))(BIT_TOP-BIT_BOTTOM) = '1' then
						count <= resize (count + to_sfixed(1,1,0)   ,BIT_TOP, BIT_BOTTOM);
						output_internal <= resize (output_internal * A,BIT_TOP, BIT_BOTTOM);
					else
						Done <= '1';
					end if;
				end if;
			end if;
	end process;
	Output <= output_internal;
end RTL;
		

